package ru.fix.multiplexer;

import java.util.Map;
import java.util.UUID;

/**
 * Настройки мультиплексора
 */
public interface MultiplexerConfig {

    /**
     * Типы сообщдений, которые будет отправлять мультиплексор по отношению к их приоритетам
     */
    Map<MessageType, Integer> registeredMessages();

    /**
     * Интервал времени, через который будет запускаться тред, отправляющий сообщения в канал
     */
    default Integer getSendingWaitingInterval() {
        return 300;
    }

    default String getName() {
        return "multiplexer " + UUID.randomUUID();
    }
}
