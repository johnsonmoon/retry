package com.github.johnsonmoon.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Retry utility for operations.
 * <p>
 * Create by johnsonmoon at 2018/11/6 09:24.
 */
@SuppressWarnings("all")
public class Retry<T> {
    private static Logger logger = LoggerFactory.getLogger(Retry.class);

    private long maxOperationWaitTime = 30_000;//default 30s
    private long retryIntervalTime = 0;//default 0s
    private int maxRetryTimes = 3;//default 3 times
    private Operation<T> operation;
    private List<Judgement<T>> judgements;

    /**
     * @param maxOperationWaitTime max wait time during single retry, in milli seconds (ms)
     * @return {@link Retry}
     */
    public Retry<T> maxOperationWaitTime(long maxOperationWaitTime) {
        this.maxOperationWaitTime = maxOperationWaitTime;
        return this;
    }

    /**
     * @param retryIntervalTime interval time between two retries, in milli seconds (ms)
     *                          <p/>Time between the end of the last retry and the beginning of the next retry.
     * @return {@link Retry}
     */
    public Retry<T> retryIntervalTime(long retryIntervalTime) {
        this.retryIntervalTime = retryIntervalTime;
        return this;
    }

    /**
     * @param maxRetryTimes max retry times
     * @return {@link Retry}
     */
    public Retry<T> maxRetryTimes(int maxRetryTimes) {
        this.maxRetryTimes = maxRetryTimes;
        return this;
    }

    /**
     * @param operation {@link Operation} operation which would be done during retries
     * @return {@link Retry}
     */
    public Retry<T> operation(Operation<T> operation) {
        this.operation = operation;
        return this;
    }

    /**
     * Add a judgement. Invoke this method multiple times to add more judgments.
     *
     * @param judgement {@link Judgement} multiple judgements turns out to be 'or' operation,
     *                  <p/>it means where there is a 'true' returned by one judgement, the operation would be retried.
     *                  <p/>If no judgements was given, then it will judged by default condition {@link IsNullJudgement}
     * @return {@link Retry}
     */
    public Retry<T> judgement(Judgement<T> judgement) {
        if (this.judgements == null) {
            this.judgements = new ArrayList<>();
        }
        this.judgements.add(judgement);
        return this;
    }

    /**
     * Do operation, retry when judgement returns 'true'.
     *
     * @return T
     */
    public T execute() {
        return execute(this.maxOperationWaitTime, this.retryIntervalTime, this.maxRetryTimes, this.operation, this.judgements);
    }

    /**
     * Do operation, retry when judgement returns 'true'.
     *
     * @param maxOperationWaitTime max wait time during single retry, in milli seconds (ms)
     *                             <p>
     * @param retryIntervalTime    interval time between two retries, in milli seconds (ms)
     *                             Time between the end of the last retry and the beginning of the next retry.
     *                             <p>
     * @param maxRetryTimes        max retry times
     *                             <p>
     * @param operation            {@link Operation} operation which would be done during retries
     *                             <p>
     * @param judgements           {@link Judgement} multiple judgements turns out to be 'or' operation,
     *                             it means where there is a 'true' returned by one judgement, the operation would be retried.
     *                             If no judgements was given, then it will judged by default condition {@link IsNullJudgement}
     *                             <p>
     * @return T
     */
    public T execute(long maxOperationWaitTime,
                     long retryIntervalTime,
                     int maxRetryTimes,
                     Operation<T> operation,
                     List<Judgement<T>> judgements) {
        if (operation == null) {
            throw new RuntimeException("Operation must not be null.");
        }
        int operationTimes = 0;
        T t = null;
        while (operationTimes <= maxRetryTimes) {
            logger.debug(operationTimes == 0 ? "Operation: {}" : "Operation retry: {}", operationTimes);
            t = waitOperate(operation, maxOperationWaitTime);
            boolean shouldRetry = false;
            if (judgements == null || judgements.size() == 0) {//default condition
                shouldRetry = new IsNullJudgement().judge(t);
            } else {
                for (Judgement<T> judgement : judgements) {
                    shouldRetry = shouldRetry || judgement.judge(t);
                }
            }
            if (!shouldRetry) {
                break;
            }
            operationTimes++;
            sleep(retryIntervalTime);
        }
        logger.debug("done.");
        return t;
    }

    /**
     * Judge whether the operation should be retried.
     */
    public interface Judgement<T> {
        /**
         * Judge whether the operation should be retried.
         *
         * @param t operation return value {@link Operation}
         * @return true: operation should be retried, false: stop retrying and return.
         */
        boolean judge(T t);
    }

    public class IsNullJudgement implements Judgement<T> {
        /**
         * Default judgement. if operation result (T)t is null,
         * <p/>then return 'true', and the operation should be retried.
         *
         * @param t operation return value {@link Operation}
         * @return true when t is null
         */
        @Override
        public boolean judge(T t) {
            return t == null;
        }
    }

    /**
     * Do something and return value.
     */
    public interface Operation<T> {
        /**
         * Do something and return value.
         *
         * @return could be null
         */
        T operate();
    }

    private T waitOperate(Operation<T> operation, long maxOperationWaitTime) {
        final Map<String, T> result = new HashMap<>();
        Future<?> future = executorService.submit(() -> result.put("t", operation.operate()));
        long start = System.currentTimeMillis();
        while (true) {
            sleep(100);
            if (future.isDone()) {
                break;
            }
            long current = System.currentTimeMillis();
            if (current - start >= maxOperationWaitTime) {
                if (!future.isDone()) {
                    boolean canceled = future.cancel(true);
                    logger.debug("Operation timeout, canceled: {}, waitTime: {}", canceled, maxOperationWaitTime);
                }
                break;
            }
        }
        return result.get("t");
    }

    private static void sleep(long sleepTimeMillis) {
        try {
            TimeUnit.MILLISECONDS.sleep(sleepTimeMillis);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private static ExecutorService executorService = Executors.newFixedThreadPool(6);
}
