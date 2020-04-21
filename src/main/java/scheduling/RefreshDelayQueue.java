package scheduling;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 可刷新的延迟队列
 *
 * @author xuanyangyang
 * @since 2020/4/15 16:18
 */
public class RefreshDelayQueue<E extends Delayed> extends DelayQueue<E> {
    private final transient ReentrantLock lock;

    private RefreshDelayQueue() throws NoSuchFieldException, IllegalAccessException {
        Field lockField = DelayQueue.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        lock = (ReentrantLock) lockField.get(this);
    }

    /**
     * 刷新任务时间
     */
    public void refresh() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            List<E> tempTasks = new ArrayList<E>(this);
            clear();
            addAll(tempTasks);
        } finally {
            lock.unlock();
        }
    }

    public static <T extends Delayed> RefreshDelayQueue<T> newQueue() throws NoSuchFieldException, IllegalAccessException {
        return new RefreshDelayQueue<>();
    }
}
