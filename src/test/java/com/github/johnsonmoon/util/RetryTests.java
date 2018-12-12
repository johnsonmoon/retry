package com.github.johnsonmoon.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Create by johnsonmoon at 2018/11/6 14:29.
 */
public class RetryTests {
    private static Logger logger = LoggerFactory.getLogger(RetryTests.class);

    /**
     * block 10s
     */
    private String mockOperation() {
        long start = System.currentTimeMillis();
        int i = 0;
        for (; i < 10; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
        return String.format("Generate value: %s, cost: %sms", i, (System.currentTimeMillis() - start));
    }

    @Test
    public void test1() {
        Retry<String> retry = new Retry<>();
        String result = retry.execute(
                5000,
                3000,
                3,
                this::mockOperation,
                null
        );
        System.out.println(result);
    }

    @Test
    public void test2() {
        Retry<String> retry = new Retry<>();
        String result = retry.execute(
                11_000,
                3000,
                3,
                this::mockOperation,
                Collections.singletonList(t -> (t == null || t.isEmpty()))
        );
        System.out.println(result);
    }

    @Test
    public void test3() {
        String result = new Retry<String>()
                .maxOperationWaitTime(9_900)
                .retryIntervalTime(0)
                .maxRetryTimes(3)
                .operation(this::mockOperation)
                .judgement(t -> (t == null || t.isEmpty()))
                .execute();
        System.out.println(result);
    }

    @Test
    public void singleTest() {
        String result = new Retry<String>()
                .maxOperationWaitTime(30_000)//Max operation wait time during a single operation
                .retryIntervalTime(1_000)//Interval time between two operations
                .maxRetryTimes(3)//Retry times when operation failed(or timeout) at the first time
                .operation(() -> {
                    //your operation
                    return "success!";
                })
                .judgement(t -> (t == null || t.isEmpty()))//add your judgement whether the operation should be retry(Operation should return a value)
                .execute();
        System.out.println(result);
    }
}
