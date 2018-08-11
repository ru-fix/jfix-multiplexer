package ru.fix.multiplexer;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * Input Multiplexer Channel. Allows to send some messages to OutputChannel
 * @param <MsgType>
 * @param <ReturnType>
 */
public interface MultiplexerInput<MsgType, ReturnType> {

    CompletableFuture<MultiplexedMessageSendingResult<ReturnType>> send(
            MsgType msg,
            MessageType messageType,
            ExpirationDate expirationDate
    );

    CompletableFuture<MultiplexedMessageSendingResult<ReturnType>> send(
            MsgType msg,
            MessageType messageType,
            Date expirationDate
    );
}
