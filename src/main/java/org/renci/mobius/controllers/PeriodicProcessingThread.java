package org.renci.mobius.controllers;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class PeriodicProcessingThread implements Runnable {

    public static final int MAX_DEFAULT_WAIT_TIME = 60; // seconds
    public static final int DEFAULT_PERIODIC_THREAD_PERIOD = 60; // seconds

    protected static Lock syncLock = new ReentrantLock();
    protected static int waitTimeInt = 0;
    protected static int periodTimeInt = 0;

    private boolean running;

    public PeriodicProcessingThread() {
        ;
    }

    @Override
    public void run() {
        synchronized (this) {
            if (running) {
                System.out.println("PeriodicProcessingThread ran into itself, leaving");
                return;
            }
            running = true;
        }
        System.out.println("PeriodicProcessingThread executing cycle");

        String patFileName = MobiusConfig.getInstance().getEnablePeriodicProcessingFile();
        if (patFileName == null) {
            return;
        }

        File rf = new File(patFileName);
        if (!rf.exists()) {
            System.out.println("PeriodicProcessingThread enable file " + patFileName + " doesn't exist, skipping");
            running = false;
            return;
        }

        try {
            getLock();
            System.out.println("PeriodicProcessingThread executing cycle, lock aquired");
            MobiusController.getInstance().doPeriodic();
            System.out.println("PeriodicProcessingThread completed sync");
        } catch (Exception e) {
            System.out.println("PeriodicProcessingThread exception");
        } finally {
            releaseLock();
            running = false;
        }
    }


    public static void getLock() {
        syncLock.lock();
    }

    public static boolean tryLock(int sec) {
        boolean ret = false;
        try {
            ret = syncLock.tryLock(sec, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            System.out.println("PeriodicProcessingThread.tryLock interrupted externally");
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
                System.out.println("getPropertyOrDefault unable to parse property " + pName + ": " + pVal + ", using default "
                        + defaultVal);
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

