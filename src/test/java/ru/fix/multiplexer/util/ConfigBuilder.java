package ru.fix.multiplexer.util;

import ru.fix.multiplexer.MessageType;
import ru.fix.multiplexer.MultiplexerConfig;
import ru.fix.multiplexer.SimpleMultiplexerConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigBuilder {

    public static MultiplexerConfig simpleWith100Priority() {
        Map<MessageType, Integer> priority = new LinkedHashMap<>();
        priority.put(new MessageType("simple"), 100);

        return new SimpleMultiplexerConfig(priority);
    }

    public static MultiplexerConfig highMediumTrivialPriority() {
        Map<MessageType, Integer> priority = new LinkedHashMap<>();
        priority.put(new MessageType("trivial"), 1);
        priority.put(new MessageType("medium"), 5);
        priority.put(new MessageType("high"), 10);

        return new SimpleMultiplexerConfig(priority);
    }
}
