package ru.fix.multiplexer.priority;

import ru.fix.multiplexer.MessageType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Классифицирует статистику прошедших сообщений.
 * По факту подсчитывает количество прошедших сообщений каждого типа.
 */
public class ClassifiedStatistic {

    private final Iterable<MessageType> statistics;

    public ClassifiedStatistic(Iterable<MessageType> statistics) {
        this.statistics = statistics;
    }

    public Map<MessageType, Integer> calculateClassifiedStatistics() {
        Map<MessageType, Integer> classifiedStatistic = new LinkedHashMap<>();
        statistics.forEach(messageType -> classifiedStatistic.merge(messageType, 1, Integer::sum));
        return classifiedStatistic;
    }
}
