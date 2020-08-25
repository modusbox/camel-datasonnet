package com.modus.camel.datasonnet.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.fail;

public class ConcurrentUtil {
    public static void testConcurrent(Callable callable, int numberOfThreads) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List jobs = new ArrayList();
        for (int i = 0; i < numberOfThreads; i++) {
            jobs.add(callable);
        }
        try {
            List<Future> results = executor.invokeAll(jobs);

            for (Future result : results) {
                String testResponse = (String)result.get();
                if (testResponse != "OK") {
                    fail(testResponse);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
