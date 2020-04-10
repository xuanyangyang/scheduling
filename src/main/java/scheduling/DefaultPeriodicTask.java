package scheduling;

import java.util.concurrent.TimeUnit;

/**
 * 默认周期任务
 *
 * @author xuanyangyang
 * @since 2020/3/27 16:05
 */
public class DefaultPeriodicTask extends AbstractPeriodicTask {
    /**
     * 延迟多久
     */
    private final long delay;
    /**
     * 周期
     */
    private final long period;
    /**
     * 时间单位
     */
    private final TimeUnit timeUnit;

    public DefaultPeriodicTask(String name, Runnable action, long delay, long period, TimeUnit timeUnit) {
        this(name, action, false, delay, period, timeUnit);
    }

    public DefaultPeriodicTask(String name, Runnable action, boolean async, long delay, long period, TimeUnit timeUnit) {
        super(name, action, async);
        this.delay = delay;
        this.period = period;
        this.timeUnit = timeUnit;
    }

    @Override
    public long nextExecuteTime() {
        return System.currentTimeMillis() + timeUnit.toMillis(period);
    }

    @Override
    protected long firstExecuteTime() {
        return System.currentTimeMillis() + timeUnit.toMillis(delay);
    }
}
