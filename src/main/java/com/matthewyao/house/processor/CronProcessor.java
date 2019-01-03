package com.matthewyao.house.processor;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author: yaokuan
 * @Date: 2019/1/1 下午10:59
 */
public class CronProcessor {

    private static final NewHouseProcessor newHouseProcessor = new NewHouseProcessor();

    private static ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间

    static class HouseTask implements Runnable{
        @Override
        public void run() {
            newHouseProcessor.start();
        }
    }

    public static void start() {
        service.scheduleAtFixedRate(new HouseTask(), 1, 30, TimeUnit.SECONDS);
    }

}
