package de.gsi.dataset.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.gsi.dataset.utils.CachedDaemonThreadFactory;

/**
 * @author rstein
 */
public class EventThreadHelper {
    private static final int MAX_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(2 * MAX_THREADS,
            CachedDaemonThreadFactory.getInstance());

    /**
     * @return event update executor service
     */
    public static ExecutorService getExecutorService() {
        return EXECUTOR_SERVICE;
    }

    /**
     * @return maximum number of threads used for event notification
     */
    public static int getMaxThreads() {
        return MAX_THREADS;
    }
}
