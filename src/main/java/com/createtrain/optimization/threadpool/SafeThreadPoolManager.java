package com.createtrain.optimization.threadpool;

import com.createtrain.optimization.config.ModConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class SafeThreadPoolManager {
    public static ExecutorService chunkIOExecutor;
    public static ExecutorService chunkLoadExecutor;
    public static ForkJoinPool entityTickPool;

    public static void initThreadPools() {
        int ioThreads = Math.max(2, ModConfig.ioThreadPoolSize);
        int loadThreads = Math.max(2, ModConfig.chunkLoadThreadPoolSize);
        int entityThreads = Math.max(2, ModConfig.entityThreadPoolSize);

        chunkIOExecutor = Executors.newFixedThreadPool(
            ioThreads,
            r -> {
                Thread t = new Thread(r, "CreateTrain-ChunkSave-Thread");
                t.setDaemon(true);
                return t;
            }
        );

        chunkLoadExecutor = Executors.newFixedThreadPool(
            loadThreads,
            r -> {
                Thread t = new Thread(r, "CreateTrain-ConcurrentChunkLoad-Thread");
                t.setDaemon(true);
                return t;
            }
        );

        entityTickPool = new ForkJoinPool(
            entityThreads,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            true
        );
    }

    public static int getActiveWorkerCount() {
        return ModConfig.ioThreadPoolSize + ModConfig.chunkLoadThreadPoolSize + ModConfig.entityThreadPoolSize;
    }

    public static void resizeThreadPool(int count) {
        ModConfig.ioThreadPoolSize = count;
        ModConfig.chunkLoadThreadPoolSize = count;
        ModConfig.entityThreadPoolSize = count;
        if (chunkIOExecutor != null && !chunkIOExecutor.isShutdown()) {
            chunkIOExecutor.shutdown();
        }
        if (chunkLoadExecutor != null && !chunkLoadExecutor.isShutdown()) {
            chunkLoadExecutor.shutdown();
        }
        if (entityTickPool != null && !entityTickPool.isShutdown()) {
            entityTickPool.shutdown();
        }
        initThreadPools();
    }
}


