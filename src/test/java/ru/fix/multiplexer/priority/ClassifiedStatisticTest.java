package ru.fix.multiplexer.priority;

import org.junit.Assert;
import org.junit.Test;
import ru.fix.multiplexer.MessageType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClassifiedStatisticTest {

    @Test
    public void whenInputStatisticsAreEmptyClassifiedStatisticsEmptyToo() throws Exception {
        List<MessageType> statistics = Collections.emptyList();
        Map<MessageType, Integer> classifiedStatistics = new ClassifiedStatistic(statistics)
                .calculateClassifiedStatistics();

        Assert.assertTrue(classifiedStatistics.isEmpty());
    }

    @Test
    public void inputStatisticsProperlyCalculates() throws Exception {
        List<MessageType> statistics = Arrays.asList(
                new MessageType("first"),
                new MessageType("second"),
                new MessageType("third"),
                new MessageType("second"),
                new MessageType("first"),
                new MessageType("first")
        );
        Map<MessageType, Integer> classifiedStatistics = new ClassifiedStatistic(statistics)
                .calculateClassifiedStatistics();

        Assert.assertEquals(new Integer(3), classifiedStatistics.get(new MessageType("first")));
        Assert.assertEquals(new Integer(2), classifiedStatistics.get(new MessageType("second")));
        Assert.assertEquals(new Integer(1), classifiedStatistics.get(new MessageType("third")));
    }
}
