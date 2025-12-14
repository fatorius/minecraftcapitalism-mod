package com.hugosouza.minecraftcapitalism.database;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DbExecutor {
    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    public static void runAsync(Runnable task) {
        EXECUTOR.submit(task);
    }
}