package scheduling;

/**
 * 周期任务
 *
 * @author xuanyangyang
 * @since 2020/3/26 18:10
 */
public interface PeriodicTask extends DelayedTask {
    void refreshExecuteTime();
}
