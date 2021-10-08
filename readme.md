Project is currently not supported and archived.  
You can find usefull tools in [jfix-stdlib](https://github.com/ru-fix/jfix-stdlib)

Multiplexer receives messages from plenty input channels and sends them to output channel smoothly.

# What for?
If you have some narrow output channel and unstable input load and do not want to block a tread for waiting until 
channel will be free you can use Multipexer. It stores output messages returns you a promise and process message
as soon as possible.

# Features
 - allows to send messages to output channel smoothly. 
 - can prioritize messages and send most important messages first
 - sends messages asynchronously and does not block sending thread 

# How to use
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
