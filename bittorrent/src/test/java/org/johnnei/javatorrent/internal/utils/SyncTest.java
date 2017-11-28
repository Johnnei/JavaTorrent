package org.johnnei.javatorrent.internal.utils;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link Sync}
 */
public class SyncTest {

    @Test
    public void testSingalAll() throws Exception {
        Lock lockMock = mock(Lock.class);
        Condition conditionMock = mock(Condition.class);

        Sync.signalAll(lockMock, conditionMock);

        verify(lockMock).lock();
        verify(lockMock).unlock();
        verify(conditionMock).signalAll();
    }

}
