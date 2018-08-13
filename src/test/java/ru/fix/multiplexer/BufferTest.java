package ru.fix.multiplexer;

import org.junit.Assert;
import org.junit.Test;
import ru.fix.commons.profiler.impl.SimpleProfiler;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

public class BufferTest {

    @Test
    public void hasMessageIfBufferIsEmptyMustReturnsFalse() {
        Buffer<String, String> buffer = new Buffer<>("BufferTest", new SimpleProfiler());
        Assert.assertFalse(buffer.hasMessage(new MessageType("some type")));
    }

    @Test
    public void hasMessagefBufferIsNotEmptyMustReturnsTrue() {
        Buffer<String, String> buffer = new Buffer<>("BufferTest", new SimpleProfiler());
        MessageType msgType = new MessageType("SimpleMessage");
        buffer.add(
                new MessageContainer<>(
                        "Long Message content",
                        msgType,
                        new CompletableFuture<>(),
                        expireInAnHour()
                ));
        Assert.assertTrue(buffer.hasMessage(msgType));
    }

    @Test
    public void addContainerToBufferWhenItEmptyAndReadIt() {
        Buffer<String, String> buffer = new Buffer<>("BufferTest", new SimpleProfiler());

        MessageContainer<String, String> originMsgContainer = new MessageContainer<>(
                "Long Message content",
                new MessageType("SimpleMessage"),
                new CompletableFuture<>(),
                expireInAnHour()
        );
        buffer.add(originMsgContainer);
        MessageContainer<String, String> receivedMsgContainer = buffer.pollNext(originMsgContainer.getMessageType());

        Assert.assertEquals(originMsgContainer, receivedMsgContainer);
    }

    @Test
    public void addContainerToBufferWhenItAlreadyContainsSomeValueWithSameTypeAndReadIt() {
        Buffer<String, String> buffer = new Buffer<>("BufferTest", new SimpleProfiler());

        MessageContainer<String, String> firstOriginMsgContainer = new MessageContainer<>(
                "Long Message content",
                new MessageType("SimpleMessage"),
                new CompletableFuture<>(),
                expireInAnHour()
        );
        buffer.add(firstOriginMsgContainer);

        MessageContainer<String, String> secondOriginMsgContainer = new MessageContainer<>(
                "Second Long Message content",
                new MessageType("SimpleMessage"),
                new CompletableFuture<>(),
                expireInAnHour()
        );
        buffer.add(secondOriginMsgContainer);

        Assert.assertEquals(firstOriginMsgContainer, buffer.pollNext(firstOriginMsgContainer.getMessageType()));
        Assert.assertEquals(secondOriginMsgContainer, buffer.pollNext(secondOriginMsgContainer.getMessageType()));
    }

    @Test
    public void addContainerToBufferWhenItAlreadyContainsSomeValueWithAnotherTypeAndReadIt() {
        Buffer<String, String> buffer = new Buffer<>("BufferTest", new SimpleProfiler());

        MessageContainer<String, String> firstOriginMsgContainer = new MessageContainer<>(
                "Long Message content",
                new MessageType("SimpleMessage"),
                new CompletableFuture<>(),
                expireInAnHour()
        );
        buffer.add(firstOriginMsgContainer);

        MessageContainer<String, String> secondOriginMsgContainer = new MessageContainer<>(
                "Second Long Message content",
                new MessageType("Not so simple message"),
                new CompletableFuture<>(),
                expireInAnHour()
        );
        buffer.add(secondOriginMsgContainer);

        Assert.assertEquals(firstOriginMsgContainer, buffer.pollNext(firstOriginMsgContainer.getMessageType()));
        Assert.assertEquals(secondOriginMsgContainer, buffer.pollNext(secondOriginMsgContainer.getMessageType()));
    }

    @Test(timeout = 1000)
    public void whenTryingToGetNextMsgWhenQueueIsEmptyReturnNull() {
        Buffer<String, String> buffer = new Buffer<>("BufferTest", new SimpleProfiler());
        Assert.assertNull(buffer.pollNext(new MessageType("some type")));
    }

    @Test
    public void whenSingleMessagePolledBufferMustBeEmpty() {
        Buffer<String, String> buffer = new Buffer<>("BufferTest", new SimpleProfiler());
        buffer.add(new MessageContainer<>(
                "Long Message content",
                new MessageType("SimpleMessage"),
                new CompletableFuture<>(),
                expireInAnHour()
        ));

        Assert.assertNotNull(buffer.pollNext(new MessageType("SimpleMessage")));
        Assert.assertTrue(buffer.isEmpty());
    }

    private ExpirationDate expireInAnHour() {
        return ExpirationDate.expiresIn(1, ChronoUnit.HOURS);
    }
}
