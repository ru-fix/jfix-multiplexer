package ru.fix.multiplexer;

public class MultiplexedMessageSendingResult<ReceivedResult> {

    private final Status status;
    private final ReceivedResult result;

    private MultiplexedMessageSendingResult(Status status, ReceivedResult result) {
        this.status = status;
        this.result = result;
    }

    public static <T> MultiplexedMessageSendingResult<T> sent(T receivedResultFromChannel) {
        return new MultiplexedMessageSendingResult<>(Status.SENT, receivedResultFromChannel);
    }

    public static <T> MultiplexedMessageSendingResult<T> notSent() {
        return new MultiplexedMessageSendingResult<>(Status.NOT_SENT, null);
    }

    public static <T> MultiplexedMessageSendingResult<T> notSentShuttingDown() {
        return new MultiplexedMessageSendingResult<>(Status.NOT_SENT_SHUTTING_DOWN, null);
    }

    public ReceivedResult getResult() {
        return result;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        SENT,
        NOT_SENT,
        NOT_SENT_SHUTTING_DOWN
    }
}
