package scheduling;

import org.springframework.scheduling.support.CronSequenceGenerator;

import java.util.Date;

/**
 * cron 任务
 *
 * @author xuanyangyang
 * @since 2020/3/27 16:09
 */
public class CronTask extends AbstractPeriodicTask {
    /**
     * cron 解析器
     */
    private final CronSequenceGenerator cronSequenceGenerator;

    public CronTask(String name, Runnable action, String cron) {
        this(name, action, false, cron);
    }

    protected CronTask(String name, Runnable action, boolean async, String cron) {
        super(name, action, async);
        cronSequenceGenerator = new CronSequenceGenerator(cron);
    }

    @Override
    public long nextExecuteTime() {
        return cronSequenceGenerator.next(new Date()).getTime();
    }

    @Override
    protected long firstExecuteTime() {
        return nextExecuteTime();
    }
}
