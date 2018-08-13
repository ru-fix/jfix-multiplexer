package ru.fix.multiplexer;

import org.junit.Assert;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

public class MessageContainerTest {

    @Test
    public void statusisExpiredwhenTtlIsNotExpiredReturnsFalse() {
        MessageContainer<String, String> msg = new MessageContainer<>(
                "Long msg",
                new MessageType("some type"),
                new CompletableFuture<>(),
                ExpirationDate.expiresIn(1000)
        );

        Assert.assertFalse(msg.isExpired());
    }

    @Test
    public void statusisExpiredwhenTtlIsExpiredReturnsTrue() throws Exception {
        MessageContainer<String, String> msg = new MessageContainer<>(
                "Long msg",
                new MessageType("some type"),
                new CompletableFuture<>(),
                ExpirationDate.expiresIn(10)
        );

        Thread.sleep(50);
        Assert.assertTrue(msg.isExpired());
    }

    @Test
    public void statusisExpiredwhenMessageWithoutExpiredNeverExpired() throws Exception {
        MessageContainer<String, String> msg = new MessageContainer<>(
                "Long msg",
                new MessageType("some type"),
                new CompletableFuture<>(),
                ExpirationDate.expiresIn(1, ChronoUnit.HOURS)
        );

        Thread.sleep(50);
        Assert.assertFalse(msg.isExpired());
    }
}
