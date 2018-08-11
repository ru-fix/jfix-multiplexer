package ru.fix.multiplexer.util;

import ru.fix.multiplexer.ExpirationDate;
import ru.fix.multiplexer.MultiplexerOutputChannel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class OutputChannelBuilder {

    public static MultiplexerOutputChannel<String, String> addWordReceived(Supplier<Boolean> channelHasFreeSlot) {
        return new MultiplexerOutputChannel<String, String>() {
            @Override
            public CompletableFuture<String> send(String message, ExpirationDate expirationTime) {
                return CompletableFuture.supplyAsync(() -> message + " received");
            }

            @Override
            public boolean hasFreeSlot() {
                return channelHasFreeSlot.get();
            }
        };
    }
}
