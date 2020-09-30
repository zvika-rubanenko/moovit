package com.moovit.poller;

import lombok.Getter;

import java.util.Date;


public class StopEtaLine {
    @Getter
    private String line;
    private StopEta stopEta;

    public StopEtaLine(StopEta stopEta, String line) {
        this.stopEta = stopEta;
        this.line = line;
    }

    public int getStopId() {
        return stopEta.getStopId();
    }

    public Date getEta() {
        return stopEta.getEta();
    }
}
