package ru.fix.multiplexer.priority;

import ru.fix.multiplexer.MessageType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Вычисляет разницу между ожидаемым распределенем и актуальным.
 * <p>
 * Если актуальный результат меньше, чем ожидаемый, будут отданы рекомендации для этого типа
 */
public class StatisticMatcher {

    private final Map<MessageType, Integer> expectedSpreading;

    private final Map<MessageType, Integer> actualSpreading;

    public StatisticMatcher(Map<MessageType, Integer> expectedSpreading, Map<MessageType, Integer> actualSpreading) {
        this.expectedSpreading = expectedSpreading;
        this.actualSpreading = actualSpreading;
    }


    public LinkedHashMap<MessageType, Integer> calculate() {
        LinkedHashMap<MessageType, Integer> recommendation = new LinkedHashMap<>();
        expectedSpreading.forEach((currentMessageType, value) -> {
            Integer difference = value - actualSpreading.getOrDefault(currentMessageType, 0);
            if (difference > 0) {
                recommendation.put(currentMessageType, difference);
            }
        });
        return recommendation;
    }
}
