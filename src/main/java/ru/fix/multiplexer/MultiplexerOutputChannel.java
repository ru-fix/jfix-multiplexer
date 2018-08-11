package ru.fix.multiplexer;

import java.util.concurrent.CompletableFuture;

/**
 * Output channel for Multiplexer
 *
 * @author Tim Urmancheev
 */
public interface MultiplexerOutputChannel<MsgType, ReturnType> {

    /**
     * Send message to output. This method can block execution,
     * in this case Multiplexer will pause processing messages
     */
    CompletableFuture<ReturnType> send(MsgType message, ExpirationDate expirationTime);

    /**
     * Output has free slot. Multiplexer will wait for free slot to send messages.
     */
    boolean hasFreeSlot();
}
