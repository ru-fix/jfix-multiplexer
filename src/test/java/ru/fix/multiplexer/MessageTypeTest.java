package ru.fix.multiplexer;

import org.junit.Assert;
import org.junit.Test;

public class MessageTypeTest {

    @Test
    public void theSameMessageTypesAreEquals() throws Exception {
        Assert.assertEquals(new MessageType("some type"), new MessageType("some type"));
    }

    @Test
    public void differentMessageTypesAreNotEquals() throws Exception {
        Assert.assertNotEquals(new MessageType("some type"), new MessageType("another type"));
    }
}
