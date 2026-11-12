package com.dlust.sportbackend.concurrency;

import com.dlust.sportbackend.Mapper.RegistrationLockMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

@Component
public class RegistrationWriteExecutor {
    private final RegistrationGate gate;
    private final RegistrationLockMapper locks;
    private final TransactionTemplate transaction;

    public RegistrationWriteExecutor(RegistrationGate gate, RegistrationLockMapper locks,
                                     PlatformTransactionManager manager) {
        this.gate = gate;
        this.locks = locks;
        this.transaction = new TransactionTemplate(manager);
        // Every quota query after waiting for locks must see the latest committed rows.
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setTimeout(15);
    }

    public <T> T execute(Collection<Long> userIds, Long eventId, Supplier<T> action) {
        if (eventId == null || userIds == null || userIds.isEmpty() || userIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("报名用户或项目不存在");
        }
        // Make transaction lifetime explicit: Redis release must occur AFTER commit/rollback.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("报名写入必须从独立事务入口调用");
        }
        List<Long> ordered = userIds.stream().distinct().sorted().toList();
        try (RegistrationGate.Lease ignored = gate.acquire(ordered, eventId)) {
            return transaction.execute(status -> {
                // Fixed order for every entry point, including batch promotion.
                for (Long userId : ordered) {
                    if (locks.lockUser(userId) == null) throw new IllegalArgumentException("用户不存在");
                }
                if (locks.lockEvent(eventId) == null) throw new IllegalArgumentException("项目不存在");
                return action.get();
            });
        }
    }
}
