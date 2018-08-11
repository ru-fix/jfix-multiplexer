package ru.fix.multiplexer;

import lombok.extern.slf4j.Slf4j;
import ru.fix.commons.profiler.ProfiledCall;
import ru.fix.commons.profiler.Profiler;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Collect messages for pending sending
 *
 * @param <MsgType>
 * @param <ReturnType>
 */
@Slf4j
class Buffer<MsgType, ReturnType> {

    private final ConcurrentMap<MessageType, Queue<ProfiledMessageContainer<MsgType, ReturnType>>> buffer =
            new ConcurrentHashMap<>();

    private final String name;
    private final Profiler profiler;

    Buffer(String name, Profiler profiler) {
        this.name = name;
        this.profiler = profiler;
    }

    /**
     * Does buffer contain message with type {@code MessageType}
     */
    public boolean hasMessage(MessageType messageType) {
        Queue<ProfiledMessageContainer<MsgType, ReturnType>> queueForCurrentType = buffer.get(messageType);
        return queueForCurrentType != null && !queueForCurrentType.isEmpty();
    }

    /**
     * Return {@code true} if buffer is empty and otherwise {@code false}
     */
    public boolean isEmpty() {
        for (Queue certainMessageTypeQueue : buffer.values()) {
            if (!certainMessageTypeQueue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return a buffer size
     */
    public int size() {
        int bufferSize = 0;
        for (Queue certainMessageTypeQueue : buffer.values()) {
            bufferSize += certainMessageTypeQueue.size();
        }
        return bufferSize;
    }

    /**
     * Add new message to buffer
     */
    public void add(MessageContainer<MsgType, ReturnType> msgContainer) {
        buffer.computeIfAbsent(msgContainer.getMessageType(), messageType -> new ConcurrentLinkedQueue<>()).add(
                new ProfiledMessageContainer<>(msgContainer, profiler.start(name))
        );
    }

    /**
     * Poll next message from buffer. If message with current type is not present return {@code null}
     */
    public MessageContainer<MsgType, ReturnType> pollNext(MessageType byType) {
        Queue<ProfiledMessageContainer<MsgType, ReturnType>> queue = buffer.get(byType);
        if (queue == null) {
            return null;
        }

        ProfiledMessageContainer<MsgType, ReturnType> item = queue.poll();
        if (item == null) {
            return null;
        }
        item.profiledCall.stop();
        return item.messageContainer;
    }

    public void pollAndProcessAllMessages(Consumer<MessageContainer<MsgType, ReturnType>> processor) {
        buffer.values()
                .forEach(queue -> {
                    ProfiledMessageContainer<MsgType, ReturnType> item;
                    while ((item = queue.poll()) != null) {
                        item.profiledCall.stop();
                        try {
                            processor.accept(item.messageContainer);
                        } catch (RuntimeException e) {
                            log.error("Batch processing. Processor failure for message {}", item.messageContainer.getMessage(), e);
                        }
                    }
                });
    }

    @Override
    public String toString() {
        return "Buffer{" +
                "bufferContainsTypes=" + Arrays.toString(buffer.keySet().toArray()) +
                '}';
    }

    private static class ProfiledMessageContainer<MsgType, ReturnType> {
        public final MessageContainer<MsgType, ReturnType> messageContainer;
        public final ProfiledCall profiledCall;

        public ProfiledMessageContainer(MessageContainer<MsgType, ReturnType> messageContainer, ProfiledCall profiledCall) {
            this.messageContainer = messageContainer;
            this.profiledCall = profiledCall;
        }
    }
}
