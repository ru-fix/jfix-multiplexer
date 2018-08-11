package ru.fix.multiplexer;

import java.util.Map;

/**
 * Конфигурация для мультиплексора, предназначенная для тестов
 */
public class SimpleMultiplexerConfig implements MultiplexerConfig {

    private final Map<MessageType, Integer> registeredMessages;

    public SimpleMultiplexerConfig(Map<MessageType, Integer> registeredMessages) {
        this.registeredMessages = registeredMessages;
    }

    @Override
    public Map<MessageType, Integer> registeredMessages() {
        return registeredMessages;
    }
}
