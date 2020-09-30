package com.moovit.poller;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IPollerImpl implements IPoller {


    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private ScheduledExecutorService executor;
    private ConcurrentHashMap<Integer, TreeMap<Date, List<String>>> writeIdToReadMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, TreeMap<Date, List<String>>> readIdToTreeMap = new ConcurrentHashMap<>();
    private BlockingQueue<StopEtaLine> stopEtaQueue = new ArrayBlockingQueue<>(1000);
    private ArrayBlockingQueue<String> linesQueue;

    @Override
    public void init(PollerConfig pollerConfig) {

        //push line number to lineQueue
        linesQueue = new ArrayBlockingQueue<>(pollerConfig.getLineNumbers().size());
        linesQueue.addAll(pollerConfig.getLineNumbers());
        String lastLine = pollerConfig.getLineNumbers().get(pollerConfig.getLineNumbers().size() - 1);

        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate((Runnable) () -> {
            // in case
            if (readIdToTreeMap.isEmpty()) {

                readIdToTreeMap = writeIdToReadMap;
            }
            boolean finish = false;
            while (!finish) {
                for (int i = 0; i < pollerConfig.getMaxConcurrency(); i++) {
                    try {
                        String lineNumber = linesQueue.take();
                        new PollerTask(lineNumber, pollerConfig.getProvider(), stopEtaQueue).start();
                        if (lineNumber.equals(lastLine)) {
                            finish = true;
                            continue;
                        }
                        linesQueue.add(lineNumber);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // assume PollerTask thread have completed their action!
            lock.writeLock().lock();

            // point readIdToTreeMap to latest writeIdToReadMap
            readIdToTreeMap = writeIdToReadMap;

            // create new writeIdToReadMap
            writeIdToReadMap = new ConcurrentHashMap<>();

            lock.writeLock().unlock();

        }, 0, pollerConfig.getPollIntervalSeconds(), TimeUnit.SECONDS);

        // write to hashtable
        new StopEtaQueueHandler(stopEtaQueue, writeIdToReadMap, lock).start();
    }

    /**
     * READ
     */
    @Override
    public List<LineEta> getStopArrivals(int stopId) {

        List<LineEta> lineEtaList = new ArrayList<>();
        lock.readLock().lock();
        TreeMap<Date, List<String>> treeMap = readIdToTreeMap.get(stopId);
        Date currentDate = new Date();
        for (Map.Entry<Date, List<String>> entry : treeMap.tailMap(currentDate).entrySet()) {
            Date date = entry.getKey();
            List<String> lineNumberList = entry.getValue();
            for (String lineNumber : lineNumberList) {
                lineEtaList.add(new LineEta(lineNumber, date));
            }
        }
        //clear old data.
        for (Date date : treeMap.headMap(currentDate).keySet()) {
            treeMap.remove(date);
        }
        lock.readLock().unlock();


        return lineEtaList;
    }


    /**
     * WRITE
     */
    private static class StopEtaQueueHandler extends Thread {

        private final BlockingQueue<StopEtaLine> queue;
        private final ConcurrentHashMap<Integer, TreeMap<Date, List<String>>> writeToHashMap;
        private final ReentrantReadWriteLock lock;

        public StopEtaQueueHandler(BlockingQueue<StopEtaLine> queue,
                                   ConcurrentHashMap<Integer, TreeMap<Date, List<String>>> writeToHashMap,
                                   ReentrantReadWriteLock lock) {
            this.queue = queue;
            this.writeToHashMap = writeToHashMap;
            this.lock = lock;
        }

        public void run() {
            while (true) {
                try {
                    StopEtaLine stopEtaLine = queue.take();

                    lock.writeLock().lock();
                    TreeMap<Date, List<String>> treeMap = writeToHashMap.computeIfAbsent(stopEtaLine.getStopId(), k -> new TreeMap<>());
                    List<String> lines = treeMap.computeIfAbsent(stopEtaLine.getEta(), k -> new ArrayList<>());
                    lines.add(stopEtaLine.getLine());
                    lock.writeLock().unlock();


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

    }
}
