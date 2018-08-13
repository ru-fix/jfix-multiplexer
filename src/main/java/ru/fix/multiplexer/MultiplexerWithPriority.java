package ru.fix.multiplexer;

import lombok.extern.slf4j.Slf4j;
import ru.fix.commons.profiler.impl.SimpleProfiler;
import ru.fix.multiplexer.exception.MessageSendingException;
import ru.fix.multiplexer.priority.StatisticStorageRecommender;
import ru.fix.commons.profiler.Profiler;
import ru.fix.stdlib.concurrency.threads.NamedExecutors;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Предназначение MultiplexerWithPriority'а - в случае ограничения ширины исходящего канала буферезировать входящие
 * сообщения из разных источнков и в порядке приоритета отсылать в обработчик.
 *
 * @param <MsgType>    Тип отправляемого в канал сообщения
 * @param <ReturnType> Возвращаемый из канала тип.OutputChannel обязан возвращать CompletableFeature<ReturnType>
 */
@Slf4j
public class MultiplexerWithPriority<MsgType, ReturnType> implements Multiplexer<MsgType, ReturnType> {

    private static final long SHUTDOWN_MAX_AWAITING_TIME = 60_000L;

    private final MultiplexerOutputChannel<MsgType, ReturnType> outputChannel;
    private final Buffer<MsgType, ReturnType> buffer;
    private final StatisticStorageRecommender recommender;

    /**
     * Нормальное название класса поможет в отладке нам
     */
    private static final class Lock {}

    private final Lock workerNotifyFlag = new Lock();
    private final ExecutorService worker;
    private final MultiplexerConfig multiplexerConfig;

    private final String name;
    private final Profiler profiler;

    private volatile State state = State.ACTIVE;

    public static <MsgType, ReturnType> Multiplexer<MsgType, ReturnType> createInstance(
            final String name,
            final MultiplexerOutputChannel<MsgType, ReturnType> outputChannel,
            final MultiplexerConfig multiplexerConfig,
            final Profiler profiler
    ) {
        MultiplexerWithPriority<MsgType, ReturnType> multiplexer = new MultiplexerWithPriority<>(
                name, outputChannel, multiplexerConfig, profiler
        );
        multiplexer.start();
        return multiplexer;
    }

    public static <MsgType, ReturnType> Multiplexer<MsgType, ReturnType> createInstance(
            final String name,
            final MultiplexerOutputChannel<MsgType, ReturnType> outputChannel,
            final MultiplexerConfig multiplexerConfig
    ) {
        return createInstance(name, outputChannel, multiplexerConfig, new SimpleProfiler());
    }

    public static <MsgType, ReturnType> Multiplexer<MsgType, ReturnType> createInstance(
            final MultiplexerOutputChannel<MsgType, ReturnType> outputChannel,
            final MultiplexerConfig multiplexerConfig
    ) {
        return createInstance("multiplexer", outputChannel, multiplexerConfig, new SimpleProfiler());
    }

    /**
     * Создание мультиплексера, который следит за тем, заполнен ли исходящий канал
     */
    protected MultiplexerWithPriority(
            final String name,
            final MultiplexerOutputChannel<MsgType, ReturnType> outputChannel,
            final MultiplexerConfig multiplexerConfig,
            final Profiler profiler
    ) {
        Objects.requireNonNull(outputChannel, "Output channel must be presented");
        Objects.requireNonNull(multiplexerConfig, "Registered messages must be presented");

        if (multiplexerConfig.registeredMessages().isEmpty()) {
            throw new RuntimeException("Required at least one registered message. Now Registered messages are empty");
        }
        this.multiplexerConfig = multiplexerConfig;

        buffer = new Buffer<>(name + ".buffer", profiler);

        this.name = name;
        this.outputChannel = outputChannel;
        this.recommender = new StatisticStorageRecommender(multiplexerConfig.registeredMessages());
        log.info("Multiplexer '{}' has been created. Registered types are: {}",
                name,
                Arrays.toString(multiplexerConfig.registeredMessages().entrySet().toArray())
        );

        //attach profilerMultiplexer has been created
        this.profiler = profiler;
        this.profiler.attachIndicator(name + ".buffer_size", () -> (long) countMessagesWaitingToProcessing());

        // create thread sending messages to channel
        worker = NamedExecutors.newSingleThreadPool(name, profiler);
    }

    public void start() {
        worker.submit(new Worker(multiplexerConfig.getSendingWaitingInterval()));
        worker.shutdown(); // when main loop is terminated we can shutdown it
    }

    /**
     * Send some message to multiplexed channel
     * <p>
     * Message does not processed immediately
     *
     * @param msg            message for sending to channel
     * @param messageType    what kind of message you sent
     * @param expirationTime when message sending is not required
     * @return promise to sent this message ASAP
     */
    @Override
    public CompletableFuture<MultiplexedMessageSendingResult<ReturnType>> send(
            MsgType msg, MessageType messageType, ExpirationDate expirationTime
    ) {
        Objects.requireNonNull(msg, "Message must be present");
        Objects.requireNonNull(messageType, "MessageType must be presented");
        if (!recommender.typeIsRegistered(messageType)) {
            throw new RuntimeException(String.format("Sent message with type %s does not registered. " +
                    "Registered types are %s", messageType, recommender));
        }

        CompletableFuture<MultiplexedMessageSendingResult<ReturnType>> promise;
        State curState = state;
        switch (curState) {
            case ACTIVE:
                promise = new CompletableFuture<>();
                buffer.add(new MessageContainer<>(msg, messageType, promise, expirationTime));

                // we do not need to await it
                promise.thenRun(this::onOutputChannelHasFreeSlot);
                break;
            case SHUTDOWN:
            case FORCE_SHUTDOWN:
                log.warn("submitting send task while multiplexer in '{}' state," +
                        " check shutdown order", curState);
                // rejecting new tasks
                promise = CompletableFuture.completedFuture(MultiplexedMessageSendingResult.notSentShuttingDown());
                break;
            default:
                log.error("submitting send task, but unknown multiplexer state '{}'", curState);
                promise = CompletableFuture.completedFuture(MultiplexedMessageSendingResult.notSentShuttingDown());
                break;
        }
        return promise;
    }

    @Override
    public CompletableFuture<MultiplexedMessageSendingResult<ReturnType>> send(
            MsgType msg, MessageType messageType, Date expirationDate
    ) {
        return send(msg, messageType, ExpirationDate.expiresOn(expirationDate.toInstant()));
    }

    /**
     * Notify multiplexer output channel can receive some messages
     */
    @Override
    public void onOutputChannelHasFreeSlot() {
        synchronized (workerNotifyFlag) {
            workerNotifyFlag.notifyAll();
        }
    }

    private void sendStoredNotificationToChannel() {
        if (buffer.isEmpty()) {
            log.trace("MultiplexerWithPriority sending process has finished cause buffer is empty");
            return;
        }

        final MessageContainer<MsgType, ReturnType> currentMessage;

        currentMessage = findMessageForProcessing(recommender.makeRecommendation());

        if (currentMessage.isExpired()) {
            profiler.call(name + ".message_expired");
            log.warn("Expired message {} will not be sent", currentMessage);
            currentMessage.getPromise().complete(MultiplexedMessageSendingResult.notSent());
            return;
        }

        log.trace("Starting to sending message {}", currentMessage);

        recommender.add(currentMessage.getMessageType());


        CompletableFuture<ReturnType> promiseFromChannel;
        try {
            promiseFromChannel = profiler.profileFuture(
                    name + ".message_sent",
                    profiledCall -> outputChannel.send(currentMessage.getMessage(), currentMessage.getExpirationTime())
            );
        } catch (Exception e) {
            log.error("There is exception occurred when message send to channel", e);
            profiler.call(name + ".message_sent_failed");

            currentMessage.getPromise().completeExceptionally(
                    new MessageSendingException("There is exception occurred when message send to channel", e)
            );
            return;
        }

        log.trace("Message {} sent to channel", currentMessage);

        promiseFromChannel.handleAsync((result, ex) -> {
            if (ex == null) {
                currentMessage.getPromise().complete(MultiplexedMessageSendingResult.sent(result));
                profiler.call(name + ".message_sent_success");
            } else {
                currentMessage.getPromise().completeExceptionally(
                        new MessageSendingException("There is exception occurred when message send to channel", ex)
                );
                profiler.call(name + ".message_sent_failed");
            }
            return currentMessage;
        });
    }

    private MessageContainer<MsgType, ReturnType> findMessageForProcessing(List<MessageType> recomendations) {
        Iterator<MessageType> recommendations = recomendations.iterator();
        log.trace("Recommendations was received: {}", recommendations);

        MessageContainer<MsgType, ReturnType> currentMessage = null;
        while (null == currentMessage && recommendations.hasNext()) {
            currentMessage = buffer.pollNext(recommendations.next());
        }

        if (null == currentMessage) {
            throw new RuntimeException(
                    String.format("Can`t find any message for polling from buffer. " +
                                    "But buffer is not empty Buffer size is %s, Buffer contains: %s. Registered types are: %s",
                            buffer.size(), buffer, recommender));
        }

        return currentMessage;
    }

    @Override
    public int countMessagesWaitingToProcessing() {
        return buffer.size();
    }

    private boolean hasMessageAndPossibleToSendToChannel() {
        return !buffer.isEmpty() && outputChannel.hasFreeSlot();
    }

    @Override
    public void shutdown() {
        state = State.SHUTDOWN;
        synchronized (workerNotifyFlag) {
            workerNotifyFlag.notifyAll();
        }
        log.info("multiplexer entering shutdown state, buffer size {}", buffer.size());
    }

    @Override
    public void shutdownNow() {
        state = State.FORCE_SHUTDOWN;
        synchronized (workerNotifyFlag) {
            workerNotifyFlag.notifyAll();
        }
        log.info("multiplexer entering force_shutdown state, buffer size {}", buffer.size());
    }

    @Override
    public void close() {
        shutdownNow();
        try {
            if (!worker.awaitTermination(SHUTDOWN_MAX_AWAITING_TIME, TimeUnit.MILLISECONDS)) {
                log.error("Failed to await multiplexer '{}' termination for {} ms. Force shutdown.",
                        name, SHUTDOWN_MAX_AWAITING_TIME);
                worker.shutdownNow();
            }
        } catch (InterruptedException exc) {
            log.error("Shutdown multiplexer '{}' failed due to interruption exception.", name, exc);
            Thread.currentThread().interrupt();
            if (!worker.isShutdown()) {
                worker.shutdownNow();
            }
        }
        profiler.detachIndicator(name + ".buffer_size");
    }

    public enum State {
        /**
         * Accepts new tasks, processing submitted
         */
        ACTIVE,
        /**
         * Previously submitted task are processed, but no new tasks will be accepted
         */
        SHUTDOWN,
        /**
         * Previously submitted task are forcibly completed, no new tasks will be accepted
         */
        FORCE_SHUTDOWN
    }

    private class Worker implements Runnable {

        private Integer sendingWaitingInterval;

        public Worker(Integer sendingWaitingInterval) {
            this.sendingWaitingInterval = sendingWaitingInterval;
        }

        @Override
        public void run() {
            boolean nextLoop = true;
            while (nextLoop && !Thread.interrupted()) {
                MultiplexerWithPriority.State curState = state;
                switch (curState) {
                    case ACTIVE:
                        log.trace("MultiplexerWithPriority sending process running");
                        try {
                            while (hasMessageAndPossibleToSendToChannel()) {
                                sendStoredNotificationToChannel();
                            }
                        } catch (Exception e) {
                            log.error("Failed sending message to channel", e); // show must go on
                            continue;
                        }
                        if (state != MultiplexerWithPriority.State.ACTIVE) {
                            continue;
                        }
                        // state is still ACTIVE, time to wait a little
                        synchronized (workerNotifyFlag) {
                            // check if can send data
                            if (hasMessageAndPossibleToSendToChannel()) {
                                continue;
                            }
                            try {
                                workerNotifyFlag.wait(sendingWaitingInterval);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new MessageSendingException("Multiplexer daemon has an error", e);
                            }
                        }
                        break;
                    case SHUTDOWN:
                        try {
                            while (hasMessageAndPossibleToSendToChannel()) {
                                sendStoredNotificationToChannel();
                            }
                        } catch (Exception e) {
                            log.error("Failed sending message to channel", e); // show must go on
                            continue;
                        }
                        nextLoop = false;
                        break;
                    case FORCE_SHUTDOWN:
                        AtomicInteger forceCompleted = new AtomicInteger();
                        do {
                            buffer.pollAndProcessAllMessages(container -> {
                                container.getPromise().complete(MultiplexedMessageSendingResult.notSentShuttingDown());
                                forceCompleted.incrementAndGet();
                            });
                            if (buffer.isEmpty()) {
                                log.info("Force completed on FORCE_SHUTDOWN: {} task(s)", forceCompleted.get());
                                nextLoop = false;
                                break;
                            }
                            log.warn("buffer not drained after forced completion");
                        } while (true);
                        break;
                    default:
                        log.error("unexpected state '{}' of multiplexer '{}'", curState, name);
                        nextLoop = false;
                        break;
                }
            }
        }
    }
}