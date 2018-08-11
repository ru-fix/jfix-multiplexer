package ru.fix.multiplexer;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class MessageType {
    private final String msgType;

    public MessageType(String msgType) {
        this.msgType = msgType;
    }

    @Override
    public String toString() {
        return msgType;
    }
}

