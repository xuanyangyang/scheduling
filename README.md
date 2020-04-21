# 定时任务
用于需要调时间测试的场景
例如游戏里的定时活动，xxx活动需要在每天12点开始，然后将系统时间调至11:59等待一会即可
```java
        ScheduledService scheduledService = new DefaultScheduledService(Executors.newCachedThreadPool());
        scheduledService.addTask("xxx","0 0 12 * * ?",() -> System.out.println("活动开始"));
```