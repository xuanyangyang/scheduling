package scheduling;

import java.util.concurrent.TimeUnit;

/**
 * 默认延迟任务
 *
 * @author xuanyangyang
 * @since 2020/3/27 15:08
 */
public class DefaultDelayedTask extends AbstractDelayedTask {
    /**
     * 延迟多久
     */
    private final long delay;
    /**
     * 时间单位
     */
    private final TimeUnit timeUnit;

    public DefaultDelayedTask(String name, Runnable action, long delay, TimeUnit timeUnit) {
        this(name, action, false, delay, timeUnit);
    }

    protected DefaultDelayedTask(String name, Runnable action, boolean async, long delay, TimeUnit timeUnit) {
        super(name, action, async);
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    @Override
    protected long firstExecuteTime() {
        return System.currentTimeMillis() + timeUnit.toMillis(delay);
    }
}
