package org.johnnei.javatorrent.internal.utils;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests {@link Sync}
 */
public class SyncTest {

    @Test
    public void testSingalAll() throws Exception {
        Lock lockMock = Mockito.mock(Lock.class);
        Condition conditionMock = Mockito.mock(Condition.class);

        Sync.signalAll(lockMock, conditionMock);

        Mockito.verify(lockMock).lock();
        Mockito.verify(lockMock).unlock();
        Mockito.verify(conditionMock).signalAll();
    }

}