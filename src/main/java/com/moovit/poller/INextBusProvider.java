package com.moovit.poller;

import java.util.List;

public interface INextBusProvider {
    List<StopEta> getLineEta(String lineNumber);
}
