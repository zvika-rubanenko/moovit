package com.moovit.poller;

import lombok.AllArgsConstructor;

import java.util.concurrent.BlockingQueue;

@AllArgsConstructor
public class PollerTask extends Thread {
    private final String lineNumber;
    private final INextBusProvider provider;
    private final BlockingQueue<StopEtaLine> stopEtaQueue;


    public void run() {
        for (StopEta stopEta : provider.getLineEta(lineNumber)) {
            stopEtaQueue.add(new StopEtaLine(stopEta, lineNumber));
        }
    }

}
