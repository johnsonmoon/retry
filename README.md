# Retry
operation retry tool

## Introduction
When a operation may failed to be done or consume a lot of time , retry  tool may help you retry the operation for several times, make sure that the operation would possible succeed.

## Usage

Example like: 
```
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
```

And the result is: 
```
success!
```

