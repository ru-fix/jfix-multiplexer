package ru.fix.multiplexer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;

/**
 * Allow to create expiration date based on timeout and check it for expiration
 */
public class ExpirationDate extends Date {

    private static final int SLIP_TIME_MILLIS = 50;

    /**
     * Creates date when Something will be expired based on timeout in milliseconds.
     */
    public static ExpirationDate expiresIn(final long timeoutMs) {
        return ExpirationDate.expiresIn(timeoutMs, ChronoUnit.MILLIS);
    }

    /**
     * Creates date when Something will be expired based on timeout in specified unit.
     */
    public static ExpirationDate expiresIn(long timeout, TemporalUnit unit) {
        return new ExpirationDate(Instant
                .now()
                .plus(timeout, unit)
                .toEpochMilli()
        );
    }

    /**
     * Creates date when Something will be expired at provided time.
     */
    public static ExpirationDate expiresOn(Instant expiresOn) {
        return new ExpirationDate(expiresOn.toEpochMilli());
    }

    private ExpirationDate(final long timestampWhenExpired) {
        super(timestampWhenExpired);
    }

    public boolean isExpired() {
        return this.before(new Date());
    }

    public long remainingMs() {
        return this.getTime() - System.currentTimeMillis();
    }

    /**
     * В случае, когда объект А вызывает метод Объекта Б и передает в него Х, содержащий ExpirationDate
     * и каждый из объектов асинхронно следит за тем, чтобы таймаут не наступил, а он наступает, то сначала он должен
     * наступить у объекта Б, а потом у объекта А. SLIP_TIME_MILLIS - условное число миллисекунд, которое позволит
     * сначала заэкспарить Б и передать обработку в А.
     */
    public ExpirationDate deductSlipTime() {
        return new ExpirationDate(this.getTime() - SLIP_TIME_MILLIS);
    }
}
