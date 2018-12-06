package com.amazonaws.greengrass.cddmosh.modules;

import com.amazonaws.greengrass.cddmosh.data.Topics;
import com.amazonaws.greengrass.cddmosh.handlers.InboundDataEventHandler;
import com.amazonaws.greengrass.cddmosh.handlers.StartNewMoshSessionEventHandler;
import com.amazonaws.greengrass.cddmosh.handlers.StartupHandler;
import com.google.inject.AbstractModule;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        // Any class that needs to receive messages must be set up as an eager singleton so Guice will autowire them at startup

        // StartupHandler will get a message indicating that the core has started
        bind(StartupHandler.class).asEagerSingleton();

        // Need environment provider in Topics class so it must be bound here
        bind(Topics.class);

        bind(StartNewMoshSessionEventHandler.class).asEagerSingleton();
        bind(InboundDataEventHandler.class).asEagerSingleton();
    }
}
