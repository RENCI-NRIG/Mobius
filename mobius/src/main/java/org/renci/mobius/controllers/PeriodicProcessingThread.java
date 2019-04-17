package org.renci.mobius.controllers;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * @brief class implements periodic processing thread
 *
 * @author kthare10
 */
public class PeriodicProcessingThread implements Runnable {

    public static final int MAX_DEFAULT_WAIT_TIME = 60; // seconds
    public static final int DEFAULT_PERIODIC_THREAD_PERIOD = 60; // seconds
    private static final Logger LOGGER = Logger.getLogger( PeriodicProcessingThread.class.getName() );


    protected static Lock syncLock = new ReentrantLock();
    protected static int waitTimeInt = 0;
    protected static int periodTimeInt = 0;
    private boolean running;

    public PeriodicProcessingThread() {
        ;
    }

    @Override
    public void run() {
        LOGGER.debug("run(): IN");
        synchronized (this) {
            if (running) {
                LOGGER.debug("PeriodicProcessingThread ran into itself, leaving");
                LOGGER.debug("run(): OUT");
                return;
            }
            running = true;
        }
        LOGGER.debug("PeriodicProcessingThread executing cycle");

        String patFileName = MobiusConfig.getInstance().getEnablePeriodicProcessingFile();
        if (patFileName == null) {
            return;
        }

        File rf = new File(patFileName);
        if (!rf.exists()) {
            LOGGER.debug("PeriodicProcessingThread enable file " + patFileName + " doesn't exist, skipping");
            running = false;
            return;
        }

        try {
            getLock();
            LOGGER.debug("PeriodicProcessingThread executing cycle, lock aquired");
            MobiusController.getInstance().doPeriodic();
            LOGGER.debug("PeriodicProcessingThread completed sync");
        } catch (Exception e) {
            LOGGER.debug("PeriodicProcessingThread exception");
        } finally {
            releaseLock();
            running = false;
        }
        LOGGER.debug("run(): OUT");
    }


    public static void getLock() {
        syncLock.lock();
    }

    public static boolean tryLock(int sec) {
        boolean ret = false;
        try {
            ret = syncLock.tryLock(sec, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            LOGGER.error("PeriodicProcessingThread.tryLock interrupted externally");
            ie.printStackTrace();
        }
        return ret;
    }

    public static void releaseLock() {
        try {
            syncLock.unlock();
        } catch (Exception e) {
            ;
        }
    }

    private static int getPropertyOrDefault(String pName, int defaultVal) {
        String pVal = MobiusConfig.getInstance().getProperty(pName);
        if (pVal == null) {
            return defaultVal;
        } else {
            try {
                int parseVal = Integer.parseInt(pVal);
                if (parseVal <= 0)
                    return defaultVal;
                return parseVal;
            } catch (NumberFormatException nfe) {
                LOGGER.error("getPropertyOrDefault unable to parse property " + pName + ": " + pVal + ", using default "
                        + defaultVal);
                nfe.printStackTrace();
                return defaultVal;
            }
        }
    }

    public static int getWaitTime() {
        if (waitTimeInt > 0)
            return waitTimeInt;

        waitTimeInt = getPropertyOrDefault(MobiusConfig.periodicProcessingWaitTime, MAX_DEFAULT_WAIT_TIME);
        return waitTimeInt;
    }

    public static int getPeriod() {
        if (periodTimeInt > 0)
            return periodTimeInt;

        periodTimeInt = getPropertyOrDefault(MobiusConfig.periodicProcessingPeriod, DEFAULT_PERIODIC_THREAD_PERIOD);
        return periodTimeInt;
    }
}

