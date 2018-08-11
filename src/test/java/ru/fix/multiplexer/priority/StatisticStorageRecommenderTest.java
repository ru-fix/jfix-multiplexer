package ru.fix.multiplexer.priority;

import org.junit.Assert;
import org.junit.Test;
import ru.fix.multiplexer.MessageType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StatisticStorageRecommenderTest {

    @Test(expected = RuntimeException.class)
    public void StaticRecommenderCantBeCreatedWithEmptyRegisterdTypes() throws Exception {
        Map<MessageType, Integer> registeredTypes = new HashMap<>();
        StatisticStorageRecommender ss =  new StatisticStorageRecommender(registeredTypes);
    }

    @Test
    public void whenConstructMessageTypesIsSortedDesc() throws Exception {
        Map<MessageType, Integer> registeredTypes = new HashMap<>();

        registeredTypes.put(new MessageType("NotVeryImportant"), 5);
        registeredTypes.put(new MessageType("Trivial"), 2);
        registeredTypes.put(new MessageType("VeryImportant"), 10);

        StatisticStorageRecommender ss =  new StatisticStorageRecommender(registeredTypes);
        Iterator<Map.Entry<MessageType, Integer>> iterator = ss.getExpectedSpreading().entrySet().iterator();

        Assert.assertEquals(new MessageType("VeryImportant"), iterator.next().getKey());
        Assert.assertEquals(new MessageType("NotVeryImportant"), iterator.next().getKey());
        Assert.assertEquals(new MessageType("Trivial"), iterator.next().getKey());
    }

    @Test
    public void whenConstructStatisticFrameSizeIsSumOfPriorities() throws Exception {
        Map<MessageType, Integer> registeredTypes = new HashMap<>();

        registeredTypes.put(new MessageType("NotVeryImportant"), 5);
        registeredTypes.put(new MessageType("Trivial"), 2);
        registeredTypes.put(new MessageType("VeryImportant"), 10);

        StatisticStorageRecommender ss =  new StatisticStorageRecommender(registeredTypes);
        Assert.assertEquals(17, ss.getStatistics().maxSize());
    }

    @Test
    public void when2Of3MessagesInStatisticShouldReturnRightRecommendation() throws Exception {
        Map<MessageType, Integer> expectedSpreading = new HashMap<>();

        expectedSpreading.put(new MessageType("NotVeryImportant"), 2);
        expectedSpreading.put(new MessageType("Trivial"), 1);
        expectedSpreading.put(new MessageType("VeryImportant"), 3);

        StatisticStorageRecommender recommender = new StatisticStorageRecommender(expectedSpreading);

        recommender
            .add(new MessageType("VeryImportant"))
            .add(new MessageType("NotVeryImportant"))
            .add(new MessageType("VeryImportant"))
            .add(new MessageType("VeryImportant"));

        List<MessageType> recommendation = recommender.makeRecommendation();

        Assert.assertEquals(3, recommendation.size());

        Assert.assertEquals(new MessageType("NotVeryImportant"), recommendation.toArray()[0]);
        Assert.assertEquals(new MessageType("Trivial"), recommendation.toArray()[1]);
        Assert.assertEquals(new MessageType("VeryImportant"), recommendation.toArray()[2]);
    }


    @Test
    public void whenStatisticsIsEmptyRecommendationIsPriority() throws Exception {
        Map<MessageType, Integer> expectedSpreading = new HashMap<>();

        expectedSpreading.put(new MessageType("NotVeryImportant"), 2);
        expectedSpreading.put(new MessageType("Trivial"), 1);
        expectedSpreading.put(new MessageType("VeryImportant"), 3);

        StatisticStorageRecommender recommender = new StatisticStorageRecommender(expectedSpreading);

        List<MessageType> recommendation = recommender.makeRecommendation();

        Assert.assertEquals(3, recommendation.size());

        Assert.assertEquals(new MessageType("VeryImportant"), recommendation.toArray()[0]);
        Assert.assertEquals(new MessageType("NotVeryImportant"), recommendation.toArray()[1]);
        Assert.assertEquals(new MessageType("Trivial"), recommendation.toArray()[2]);
    }
}
