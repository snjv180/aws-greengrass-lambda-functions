package com.amazonaws.greengrass.cddmosh.handlers;

import com.amazonaws.greengrass.cddmosh.data.KeyAndPort;
import com.amazonaws.greengrass.cddmosh.data.Topics;
import com.amazonaws.greengrass.cddmosh.events.DatagramSocketCreatedEvent;
import com.amazonaws.greengrass.cddmosh.events.OutboundDataEvent;
import com.amazonaws.greengrass.cddmosh.events.StartNewMoshSessionEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.timmattison.greengrass.cdd.events.GreengrassLambdaEvent;
import com.timmattison.greengrass.cdd.events.PublishObjectEvent;
import com.timmattison.greengrass.cdd.handlers.interfaces.GreengrassLambdaEventHandler;
import com.timmattison.greengrass.cdd.nativeprocesses.interfaces.NativeProcessHelper;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.datagram.DatagramSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class StartNewMoshSessionEventHandler implements GreengrassLambdaEventHandler {
    public static final String MOSH_CONNECT_REGEX = "^MOSH CONNECT ([0-9]{5}) ([^\\s]{22})$";
    public static final Pattern MOSH_CONNECT_REGEX_PATTERN = Pattern.compile(MOSH_CONNECT_REGEX);
    public static final String LOCALE_EN_US_UTF_8 = "en_US.UTF-8";
    public static final String LOCALE_EN_GB_UTF_8 = "en_GB.UTF-8";
    public static final String LOCALE = LOCALE_EN_US_UTF_8;
    private final NativeProcessHelper nativeProcessHelper;
    private final EventBus eventBus;
    private final Topics topics;
    private Optional<Vertx> vertx = Optional.empty();

    @Override
    public boolean isTopicExpected(String topic) {
        return topic.equals(topics.getNewSessionTopic());
    }

    @Override
    public void execute(GreengrassLambdaEvent greengrassLambdaEvent) {
        eventBus.post(new StartNewMoshSessionEvent());
    }

    @Subscribe
    public void startNewMoshSessionEvent(StartNewMoshSessionEvent startNewMoshSessionEvent) {
        startNewMoshSession();
    }

    private void startNewMoshSession() {
        try {
            synchronized (this) {
                if (!vertx.isPresent()) {
                    // To get rid of "Failed to create cache dir" issue
                    System.setProperty("vertx.disableFileCPResolving", "true");

                    VertxOptions vertxOptions = new VertxOptions();
                    vertx = Optional.ofNullable(Vertx.vertx(vertxOptions));
                }
            }

            List<String> stdoutLines = new ArrayList<>();
            List<String> stderrLines = new ArrayList<>();

            Consumer<String> stdoutConsumer = (line) -> stdoutLines.add(line);
            Consumer<String> stderrConsumer = (line) -> stderrLines.add(line);

            Map<String, String> environment = new HashMap<>();
            environment.put("LC_CTYPE", LOCALE);

            List<String> programAndArguments = new ArrayList<>();
            programAndArguments.add("/usr/bin/mosh-server");

            ProcessBuilder pb = new ProcessBuilder(programAndArguments);

            pb.environment().putAll(environment);

            nativeProcessHelper.getOutputFromProcess(log, pb, true, Optional.of(stdoutConsumer), Optional.of(stderrConsumer));

            Optional<String> result = stdoutLines.stream()
                    .filter(line -> line.matches(MOSH_CONNECT_REGEX))
                    .findFirst();

            int port;
            String key;

            if (result.isPresent()) {
                Matcher matcher = MOSH_CONNECT_REGEX_PATTERN.matcher(result.get());
                matcher.find();
                port = Integer.parseInt(matcher.group(1));
                key = matcher.group(2);

                eventBus.post(PublishObjectEvent.builder().topic(topics.getOutputTopic()).object(KeyAndPort.builder().key(key).port(port).build()).build());

                int listenPort = port + 4096;
                log.info("Starting server on [" + listenPort + "]");
                DatagramSocket datagramSocket = vertx.get().createDatagramSocket();
                // Take any inbound data and turn it into an event
                datagramSocket.handler(event -> {
//                    log.info("Sending data back");
                    eventBus.post(OutboundDataEvent.builder().port(event.sender().port()).data(event.data().getBytes()).build());
                });
                datagramSocket.listen(listenPort, "127.0.0.1", s -> {
                });
                log.info("Server started on [" + listenPort + "]");
                eventBus.post(DatagramSocketCreatedEvent.builder().datagramSocket(datagramSocket).sendPort(port).listenPort(listenPort).build());
            } else {
                log.error("START mosh failed to start");
                log.error("stdout:");
                log.error(String.join("\n", stdoutLines));
                log.error("stderr:");
                log.error(String.join("\n", stderrLines));
                log.error("END   mosh failed to start");
            }
        } catch (Exception e) {
            log.info("Exception [" + e.getMessage() + "]");
        }
    }

    @Subscribe
    public void outboundDataEvent(OutboundDataEvent outboundDataEvent) {
        Map data = new HashMap();
        data.put("data", Base64.getEncoder().encodeToString(outboundDataEvent.getData()));
        eventBus.post(PublishObjectEvent.builder().topic(topics.getDataOutputTopic(String.valueOf(outboundDataEvent.getPort()))).object(data).build());
    }
}
