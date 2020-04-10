package scheduling;

import java.util.concurrent.CompletableFuture;

/**
 * @author xuanyangyang
 * @since 2020/3/26 18:03
 */
public interface ScheduledFuture {
    /**
     * 取消任务
     *
     * @return future
     */
    CompletableFuture<Boolean> cancel();
}
