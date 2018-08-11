package ru.fix.multiplexer.priority;

import ru.fix.multiplexer.MessageType;

import java.util.List;

public interface Recommender {

    List<MessageType> makeRecommendation();

    boolean typeIsRegistered(MessageType messageType);
}
