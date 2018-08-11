package ru.fix.multiplexer.priority;

import org.junit.Assert;
import org.junit.Test;
import ru.fix.multiplexer.MessageType;

import java.util.LinkedHashMap;
import java.util.Map;

public class StatisticMatcherTest {

    @Test
    public void whenActualIsLessThenExpectedItWillBeRecomended() throws Exception {
        Map<MessageType, Integer> expected = new LinkedHashMap<>();
        expected.put(new MessageType("A"), 10);
        expected.put(new MessageType("B"), 5);
        expected.put(new MessageType("C"), 5);

        Map<MessageType, Integer> actual = new LinkedHashMap<>();
        actual.put(new MessageType("A"), 11);
        actual.put(new MessageType("B"), 3);
        actual.put(new MessageType("C"), 5);

        StatisticMatcher statisticMatcher = new StatisticMatcher(expected, actual);
        LinkedHashMap<MessageType, Integer> recommendation = statisticMatcher.calculate();

        Assert.assertTrue(recommendation.containsKey(new MessageType("B")));
        Assert.assertEquals(1, recommendation.size());
        Assert.assertEquals(new Integer(2), recommendation.get(new MessageType("B")));
    }

    @Test
    public void whenActualIsTheSameExpectedRecommendationShouldBeEmpty() throws Exception {
        Map<MessageType, Integer> expected = new LinkedHashMap<>();
        expected.put(new MessageType("A"), 10);
        expected.put(new MessageType("B"), 5);
        expected.put(new MessageType("C"), 5);

        Map<MessageType, Integer> actual = expected;

        StatisticMatcher statisticMatcher = new StatisticMatcher(expected, actual);
        LinkedHashMap<MessageType, Integer> recommendation = statisticMatcher.calculate();

        Assert.assertTrue(recommendation.isEmpty());
    }

    @Test
    public void whenActualIsLessOrTheSameActualRecommendationShouldBeEmpty() throws Exception {
        Map<MessageType, Integer> expected = new LinkedHashMap<>();
        expected.put(new MessageType("A"), 10);
        expected.put(new MessageType("B"), 5);
        expected.put(new MessageType("C"), 5);

        Map<MessageType, Integer> actual = new LinkedHashMap<>();
        actual.put(new MessageType("A"), 11);
        actual.put(new MessageType("B"), 6);
        actual.put(new MessageType("C"), 5);

        StatisticMatcher statisticMatcher = new StatisticMatcher(expected, actual);
        LinkedHashMap<MessageType, Integer> recommendation = statisticMatcher.calculate();

        Assert.assertTrue(recommendation.isEmpty());
    }

    @Test
    public void whenActualContainsNotAllTypesFromExpectedItShouldBeRecommended() throws Exception {
        Map<MessageType, Integer> expected = new LinkedHashMap<>();
        expected.put(new MessageType("A"), 10);
        expected.put(new MessageType("B"), 5);
        expected.put(new MessageType("C"), 5);

        Map<MessageType, Integer> actual = new LinkedHashMap<>();
        actual.put(new MessageType("A"), 11);
        StatisticMatcher statisticMatcher = new StatisticMatcher(expected, actual);
        LinkedHashMap<MessageType, Integer> recommendation = statisticMatcher.calculate();

        Assert.assertEquals(new Integer(5), recommendation.get(new MessageType("B")));
        Assert.assertEquals(new Integer(5), recommendation.get(new MessageType("C")));
    }
}
