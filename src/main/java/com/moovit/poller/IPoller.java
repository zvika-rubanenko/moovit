package com.moovit.poller;

import java.util.List;

public interface IPoller {
    void init(PollerConfig pollerConfig);

    List<LineEta> getStopArrivals(int stopId);
}
