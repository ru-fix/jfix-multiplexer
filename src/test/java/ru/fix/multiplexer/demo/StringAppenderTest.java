package ru.fix.multiplexer.demo;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.fix.multiplexer.*;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * This is the simplest example of using multiplexer
 * It allows to append string using multiplexer
 */
public class StringAppenderTest {

    class AppendedString implements MultiplexerOutputChannel<String, Void> {

        final StringBuilder container = new StringBuilder();

        @Override
        public CompletableFuture<Void> send(String message, ExpirationDate expirationTime) {
            return CompletableFuture.supplyAsync(() -> {
                container.append(message);
                return null;
            });
        }

        @Override
        public boolean hasFreeSlot() {
            return true;
        }
    }

    @Test
    public void name() {
        // just imagine we need to append strings
        AppendedString appendedString = new AppendedString();

        // we need to register message types because multiplexer prioritize incoming messages
        // but in this case all mesages has the same priority
        MultiplexerConfig config = new SimpleMultiplexerConfig(Collections.singletonMap(new MessageType("txt"), 1));

        //create multiplexer with output channel as string appender and the registered message
        Multiplexer<String, Void> multiplexer = MultiplexerWithPriority.createInstance(appendedString, config);

        // send two massages to appender through multiplexer
        CompletableFuture<MultiplexedMessageSendingResult<Void>> helloSent =
                multiplexer.send("Hello ", new MessageType("txt"), ExpirationDate.expiresIn(10_000));
        CompletableFuture<MultiplexedMessageSendingResult<Void>>
                wordSent = multiplexer.send("word", new MessageType("txt"), ExpirationDate.expiresIn(10_000));

        // wait while messages are processing
        CompletableFuture.allOf(helloSent, wordSent).join();

        // messages has been sent and processed
        Assert.assertThat(appendedString.container.toString(), Matchers.containsString("Hello"));
        Assert.assertThat(appendedString.container.toString(), Matchers.containsString("word"));
        System.out.println("The final string: " + appendedString.container.toString());
    }
}
