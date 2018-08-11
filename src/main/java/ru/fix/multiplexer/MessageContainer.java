package ru.fix.multiplexer;

import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

@ToString
@Data
class MessageContainer<MsgType, ReturnType> {

    private final Date creationDate;

    private final MsgType message;

    private final CompletableFuture<MultiplexedMessageSendingResult<ReturnType>> promise;

    private final MessageType messageType;

    private final ExpirationDate expirationTime;

    public MessageContainer(
            MsgType message,
            MessageType messageType,
            CompletableFuture<MultiplexedMessageSendingResult<ReturnType>> promise,
            ExpirationDate expirationTime
    ) {
        this.creationDate = new Date();
        this.message = message;
        this.promise = promise;
        this.messageType = messageType;
        this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
        return expirationTime.isExpired();
    }
}
