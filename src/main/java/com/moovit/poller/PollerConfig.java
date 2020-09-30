package com.moovit.poller;

import lombok.Data;

import java.util.List;


@Data
public class PollerConfig {
    INextBusProvider provider;
    List<String> lineNumbers;
    int pollIntervalSeconds;
    int maxConcurrency;
}
