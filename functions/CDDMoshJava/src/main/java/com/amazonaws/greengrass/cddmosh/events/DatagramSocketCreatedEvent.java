package com.amazonaws.greengrass.cddmosh.events;

import io.vertx.core.datagram.DatagramSocket;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatagramSocketCreatedEvent {
    private DatagramSocket datagramSocket;

    private int sendPort;

    private int listenPort;
}
