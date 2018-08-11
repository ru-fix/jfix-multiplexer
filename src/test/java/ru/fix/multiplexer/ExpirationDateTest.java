package ru.fix.multiplexer;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Date;
import java.time.Instant;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.AllOf.allOf;

public class ExpirationDateTest {

    @Test
    public void whenExpirationDateInThePastThenItIsExpiredNow() {
        Assert.assertTrue(ExpirationDate.expiresIn(-10_000).isExpired());
    }

    @Test
    public void whenExpirationDateInTheFutureThenItIsNotExpiredNow() {
        Assert.assertFalse(ExpirationDate.expiresIn(36_000_000).isExpired());
    }

    @Test
    public void expirationDateCreatedBasedOnTheDateMustBeEquals() {
        Instant shouldExpireAt = Instant.now();
        ExpirationDate expirationDate = ExpirationDate.expiresOn(shouldExpireAt);

        Assert.assertEquals(Date.from(shouldExpireAt), expirationDate);
    }

    @Test
    public void remainingCalculatesCorrect() {
        long actualRemaining = ExpirationDate.expiresIn(100).remainingMs();
        // between. if gc occurred
        Assert.assertThat(actualRemaining, allOf(greaterThanOrEqualTo(99L), lessThanOrEqualTo(100L)));
    }

    @Test
    public void whenDeductSlipTimeNewDateLessThenOriginal() {
        ExpirationDate original = ExpirationDate.expiresIn(1000);
        Assert.assertTrue(original.deductSlipTime().remainingMs() < original.remainingMs());
    }
}
