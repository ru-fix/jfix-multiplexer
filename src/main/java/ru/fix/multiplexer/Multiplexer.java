package ru.fix.multiplexer;

import java.util.concurrent.ExecutorService;

public interface Multiplexer<MsgType, ReturnType> extends
        MultiplexerInput<MsgType, ReturnType>,
        MultiplexerProcessorable,
        AutoCloseable {

    int countMessagesWaitingToProcessing();

    /**
     * Submit shutdown request. The method doesn't wait multiplexer's shutdown.
     * Previously submitted tasks are executed, but no new tasks will be accepted.
     *
     * @see ExecutorService#shutdown()
     */
    default void shutdown() {
    }

    /**
     * Submit shutdownNow request.
     * The method doesn't wait multiplexer's shutdown,
     * use {@link Multiplexer#close()} instead
     *
     * @see ExecutorService#shutdownNow()
     */
    default void shutdownNow() {
    }

    /**
     * Force shutdown and wait the multiplexer finished, all submitted tasks will be force completed
     */
    @Override
    default void close() {
    }
}
