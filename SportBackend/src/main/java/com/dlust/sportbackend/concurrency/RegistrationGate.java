package com.dlust.sportbackend.concurrency;

import java.util.List;

/** Redis only controls admission. MySQL remains the source of truth for quotas. */
public interface RegistrationGate {
    Lease acquire(List<Long> userIds, Long eventId);

    interface Lease extends AutoCloseable {
        @Override
        void close();
    }
}
