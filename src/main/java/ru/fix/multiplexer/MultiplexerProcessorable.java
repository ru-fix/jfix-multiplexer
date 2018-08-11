package ru.fix.multiplexer;

/**
 * Allows to Push messages from Multiplexer Buffer to output channel
 */
public interface MultiplexerProcessorable {

    void onOutputChannelHasFreeSlot();
}
