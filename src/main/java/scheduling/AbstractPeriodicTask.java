package scheduling;

/**
 * 抽象周期任务
 *
 * @author xuanyangyang
 * @since 2020/3/27 15:59
 */
public abstract class AbstractPeriodicTask extends AbstractDelayedTask implements PeriodicTask {
    public AbstractPeriodicTask(String name, Runnable action) {
        super(name, action);
    }

    protected AbstractPeriodicTask(String name, Runnable action, boolean async) {
        super(name, action, async);
    }

    @Override
    public void run() {
        try {
            super.run();
        } finally {
            refreshExecuteTime();
        }
    }

    /**
     * @return 下一次执行时间
     */
    public abstract long nextExecuteTime();

    @Override
    public void refreshExecuteTime() {
        setExecuteTime(nextExecuteTime());
    }
}
