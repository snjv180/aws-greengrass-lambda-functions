package com.amazonaws.greengrass.cddmosh.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OutboundDataEvent {
    private int port;

    private byte[] data;
}
