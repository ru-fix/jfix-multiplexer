package ru.fix.multiplexer.priority;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import ru.fix.multiplexer.MessageType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Высчитывает рекомендации о том какой тип сообщения необходимо отправить в канал на основе статистики прошедших
 * через канал сообщений и приоритетов сообщений.
 */
public class StatisticStorageRecommender implements Recommender {

    /**
     * Прошедшие через канал сообщения.
     */
    private CircularFifoQueue<MessageType> statistics;

    /**
     * идеальное распределение тип сообщения к их количеству в канале
     */
    private LinkedHashMap<MessageType, Integer> expectedSpreading;

    public StatisticStorageRecommender(final Map<MessageType, Integer> expectedSpreading) {
        this.expectedSpreading = expectedSpreading.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        int countOfElements = this.expectedSpreading
                .values()
                .stream()
                .reduce((sum, element) -> sum += element)
                .orElseThrow(() -> new RuntimeException("Cant calculate statistic capacity. "
                        + "May be registered types are not present. Registered types are " + expectedSpreading.size())
                );
        statistics = new CircularFifoQueue<>(countOfElements);
    }

    public StatisticStorageRecommender add(MessageType messageType) {
        statistics.add(messageType);
        return this;
    }

    /**
     * Отдает рекомендации о том, какие сообщения должны быть отправлены
     * Сообщения сортированы по важности. Первым идет самое приоритетное для отправки, последним - наименее приоритетное
     */
    @Override
    public List<MessageType> makeRecommendation() {
        Map<MessageType, Integer> classifiedStatistics = new ClassifiedStatistic(statistics)
                .calculateClassifiedStatistics();
        LinkedHashMap<MessageType, Integer> recommendations = new StatisticMatcher(expectedSpreading, classifiedStatistics)
                .calculate();
        expectedSpreading.forEach(recommendations::putIfAbsent);
        return new ArrayList<>(recommendations.keySet());
    }

    @Override
    public boolean typeIsRegistered(MessageType messageType) {
        return expectedSpreading.keySet().contains(messageType);
    }

    public CircularFifoQueue<MessageType> getStatistics() {
        return statistics;
    }

    public Map<MessageType, Integer> getExpectedSpreading() {
        return expectedSpreading;
    }

    @Override
    public String toString() {
        return "StatisticStorageRecommender{" +
                "statistics=" + Arrays.toString(statistics.toArray()) +
                ", expectedSpreading=" + Arrays.toString(expectedSpreading.entrySet().toArray()) +
                '}';
    }
}
