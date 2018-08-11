package ru.fix.multiplexer;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Мультиплексор, который ничего не мультиплексирует, а просто отправляет в канал без каких-либо преобразований
 * Может быть полезен при тестировании
 *
 * @param <MsgType>
 * @param <ReturnType>
 */
public class FakeMultiplexer<MsgType, ReturnType> implements Multiplexer<MsgType, ReturnType>, AutoCloseable {

    private final BiFunction<MsgType, ExpirationDate, CompletableFuture<ReturnType>> outputChannel;

    public FakeMultiplexer(BiFunction<MsgType, ExpirationDate, CompletableFuture<ReturnType>> outputChannel) {
        this.outputChannel = outputChannel;
    }

    @Override
    public int countMessagesWaitingToProcessing() {
        return 0;
    }

    @Override
    public CompletableFuture<MultiplexedMessageSendingResult<ReturnType>> send(
            MsgType msg, MessageType messageType, ExpirationDate expirationDate
    ) {
        return outputChannel
                .apply(msg, expirationDate)
                .thenApplyAsync(MultiplexedMessageSendingResult::sent);
    }

    @Override
    public CompletableFuture<MultiplexedMessageSendingResult<ReturnType>> send(
            MsgType msg, MessageType messageType, Date expirationDate
    ) {
        return send(msg, messageType, ExpirationDate.expiresOn(expirationDate.toInstant()));
    }

    @Override
    public void onOutputChannelHasFreeSlot() {

    }
}
