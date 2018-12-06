package com.amazonaws.greengrass.cddmosh.handlers;

import com.amazonaws.greengrass.cddmosh.data.Topics;
import com.google.common.eventbus.EventBus;
import com.timmattison.greengrass.cdd.events.GreengrassStartEvent;
import com.timmattison.greengrass.cdd.events.PublishMessageEvent;
import com.timmattison.greengrass.cdd.handlers.interfaces.GreengrassStartEventHandler;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class StartupHandler implements GreengrassStartEventHandler {
    private final EventBus eventBus;
    private final Topics topics;

    @Override
    public void execute(GreengrassStartEvent greengrassStartEvent) {
        eventBus.post(PublishMessageEvent.builder().topic(topics.getOutputTopic()).message("CDD Mosh started [" + System.currentTimeMillis() + "]").build());
    }
}
