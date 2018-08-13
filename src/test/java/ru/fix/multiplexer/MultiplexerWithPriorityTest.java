package ru.fix.multiplexer;

import org.junit.Ignore;
import org.junit.Test;
import ru.fix.multiplexer.util.ConfigBuilder;
import ru.fix.multiplexer.util.OutputChannelBuilder;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class MultiplexerWithPriorityTest {
    
    @Test(timeout = 1000)
    public void multiplexerReturnsProperResultWithSimpleOutputChannel() throws Exception {
        Multiplexer<String, String> multiplexer = MultiplexerWithPriority.createInstance(
                OutputChannelBuilder.addWordReceived(() -> true),
                ConfigBuilder.simpleWith100Priority()
        );
        CompletableFuture<MultiplexedMessageSendingResult<String>> promise =
                multiplexer.send("hello", new MessageType("simple"), ExpirationDate.expiresIn(1000));

        assertEquals(MultiplexedMessageSendingResult.Status.SENT, promise.get().getStatus());
        assertEquals("hello received", promise.get().getResult());
    }

    @Test(timeout = 1000)
    public void multiplexerDoesNotProcessedExpiredMessages() throws Exception {
        Multiplexer<String, String> multiplexer = MultiplexerWithPriority.createInstance(
                OutputChannelBuilder.addWordReceived(() -> true),
                ConfigBuilder.simpleWith100Priority()
        );

        CompletableFuture<MultiplexedMessageSendingResult<String>> promiseWithExpiredTTL =
                multiplexer.send("TTL is expired", new MessageType("simple"), ExpirationDate.expiresIn(-100));
        CompletableFuture<MultiplexedMessageSendingResult<String>> processedPromise =
                multiplexer.send("TTL is not expired", new MessageType("simple"), ExpirationDate.expiresIn(500));

        Thread.sleep(150);


        assertEquals(MultiplexedMessageSendingResult.Status.NOT_SENT, promiseWithExpiredTTL.get().getStatus());
        assertEquals(MultiplexedMessageSendingResult.Status.SENT, processedPromise.get().getStatus());
    }

    @Ignore("CPAPSM-9337 [CHECKD] Нестабильный MultiplexerWithPriorityTest")
    @Test(timeout = 1000)
    public void multiplexerDoesNotProcessMessageWhenChannelIsBusy() throws Exception {
        AtomicInteger iteration = new AtomicInteger(0);
        Supplier<Boolean> hasFreeSlot = () -> iteration.getAndIncrement() == 0;

        Multiplexer<String, String> multiplexer = MultiplexerWithPriority.createInstance(
                OutputChannelBuilder.addWordReceived(hasFreeSlot),
                ConfigBuilder.simpleWith100Priority()
        );

        CompletableFuture<MultiplexedMessageSendingResult<String>> promise1 =
                multiplexer.send("Will be processed", new MessageType("simple"), ExpirationDate.expiresIn(500));
        CompletableFuture<MultiplexedMessageSendingResult<String>> promise2 =
                multiplexer.send("Will not be processed", new MessageType("simple"), ExpirationDate.expiresIn(500));

        assertEquals(MultiplexedMessageSendingResult.Status.SENT, promise1.get().getStatus());
        assertFalse(promise2.isDone());
    }

    @Test(timeout = 1000)
    public void allMessagesShouldBeSentFromBufferWhenChannelWillBeAvailable() throws Exception {
        AtomicBoolean hasFreeSlotBoolean = new AtomicBoolean(false);

        Multiplexer<String, String> multiplexer = MultiplexerWithPriority.createInstance(
                OutputChannelBuilder.addWordReceived(hasFreeSlotBoolean::get),
                ConfigBuilder.simpleWith100Priority()
        );

        CompletableFuture<MultiplexedMessageSendingResult<String>> promise1 =
                multiplexer.send("Will be processed", new MessageType("simple"), ExpirationDate.expiresIn(500));
        CompletableFuture<MultiplexedMessageSendingResult<String>> promise2 =
                multiplexer.send("Will be processed also", new MessageType("simple"), ExpirationDate.expiresIn(500));

        assertFalse(promise1.isDone());
        assertFalse(promise2.isDone());

        hasFreeSlotBoolean.set(true);

        assertEquals(MultiplexedMessageSendingResult.Status.SENT, promise1.get().getStatus());
        assertEquals(MultiplexedMessageSendingResult.Status.SENT, promise2.get().getStatus());
    }

    @Test(timeout = 1000)
    public void whenChannelIsFullTriggerDoesNotSendAnyMessage() {
        AtomicBoolean hasFreeSlotBoolean = new AtomicBoolean(false);

        Multiplexer<String, String> multiplexer = MultiplexerWithPriority.createInstance(
                OutputChannelBuilder.addWordReceived(hasFreeSlotBoolean::get),
                ConfigBuilder.simpleWith100Priority()
        );

        CompletableFuture<MultiplexedMessageSendingResult<String>> promise1 =
                multiplexer.send("Can be processed", new MessageType("simple"), ExpirationDate.expiresIn(500));

        assertFalse(promise1.isDone());
        multiplexer.onOutputChannelHasFreeSlot();
        assertFalse(promise1.isDone()); //does not sent
    }

    @Test(expected = RuntimeException.class)
    public void multiplexerDoesNotAllowToSendUnregisteredMessage() {
        Multiplexer<String, String> multiplexer = MultiplexerWithPriority.createInstance(
                OutputChannelBuilder.addWordReceived(() -> true),
                ConfigBuilder.simpleWith100Priority()
        );

        multiplexer.send("Bad message", new MessageType("unregistered type"), ExpirationDate.expiresIn(500));
    }

    @Test
    public void multiplexerMustSentHighPriorityMessagesFirstThenMediumThenTrivial() {
        // prepare a lot of messages
        Map<String, MessageType> messagesToSend = new LinkedHashMap<>();
        messagesToSend.put("trivial1", new MessageType("trivial"));
        messagesToSend.put("trivial2", new MessageType("trivial"));
        messagesToSend.put("trivial3", new MessageType("trivial"));
        messagesToSend.put("high1", new MessageType("high"));
        messagesToSend.put("high2", new MessageType("high"));
        messagesToSend.put("high3", new MessageType("high"));
        messagesToSend.put("medium1", new MessageType("medium"));
        messagesToSend.put("medium2", new MessageType("medium"));
        messagesToSend.put("medium3", new MessageType("medium"));

        // output Channel must store message ordering
        List<String> actualOrdering = new ArrayList<>();

        // do not send messages to output channel immediately
        AtomicBoolean hasFreeSlotBoolean = new AtomicBoolean(false);

        MultiplexerOutputChannel<String, String> outputChannel = new MultiplexerOutputChannel<String, String>() {
            @Override
            public CompletableFuture<String> send(String message, ExpirationDate expirationTime) {
                actualOrdering.add(message); //store sending ordering
                return CompletableFuture.supplyAsync(() -> message);
            }

            @Override
            public boolean hasFreeSlot() {
                return hasFreeSlotBoolean.get();
            }
        };

        Multiplexer<String, String> multiplexer = MultiplexerWithPriority.createInstance(
                outputChannel,
                ConfigBuilder.highMediumTrivialPriority()
        );

        // add messages to buffer
        ArrayList<CompletableFuture<MultiplexedMessageSendingResult<String>>> promises = new ArrayList<>();
        messagesToSend.forEach((key, value) -> promises.add(multiplexer.send(key, value, ExpirationDate.expiresIn(1, ChronoUnit.HOURS))));

        // output channel is free now
        hasFreeSlotBoolean.set(true);

        //waiting until all features will be completed
        CompletableFuture<Void> pr = CompletableFuture.allOf(promises.toArray(new CompletableFuture<?>[0]));
        pr.join();

        // first of all high messages, then medium, then trivial
        assertTrue(actualOrdering.get(0).startsWith("high"));
        assertTrue(actualOrdering.get(1).startsWith("high"));
        assertTrue(actualOrdering.get(2).startsWith("high"));
        assertTrue(actualOrdering.get(3).startsWith("medium"));
        assertTrue(actualOrdering.get(4).startsWith("medium"));
        assertTrue(actualOrdering.get(5).startsWith("medium"));
        assertTrue(actualOrdering.get(6).startsWith("trivial"));
        assertTrue(actualOrdering.get(7).startsWith("trivial"));
        assertTrue(actualOrdering.get(8).startsWith("trivial"));
    }
}
