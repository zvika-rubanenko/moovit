package com.moovit.poller;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;


@Data
@AllArgsConstructor
public class LineEta {
    String lineNumber;
    Date eta;
}
