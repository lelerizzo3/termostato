package com.termostato.web;

import com.termostato.persistence.ErrorLogRecord;
import com.termostato.persistence.ErrorLogRepository;
import com.termostato.persistence.PollingLogRecord;
import com.termostato.persistence.PollingLogRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/log")
public class LogController {

    private final PollingLogRepository pollingLogs;
    private final ErrorLogRepository errorLogs;
    private final Clock clock;

    public LogController(PollingLogRepository pollingLogs,
                         ErrorLogRepository errorLogs,
                         Clock clock) {
        this.pollingLogs = pollingLogs;
        this.errorLogs = errorLogs;
        this.clock = clock;
    }

    @GetMapping
    public List<PollingLogRecord> getPollingLogs(
            @RequestParam(name = "da", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "a", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UtcDateRange range = UtcDateRange.resolve(from, to, clock);
        return pollingLogs.findBetween(range.fromInclusive(), range.toExclusive());
    }

    @GetMapping("/errori")
    public List<ErrorLogRecord> getErrorLogs(
            @RequestParam(name = "da", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "a", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UtcDateRange range = UtcDateRange.resolve(from, to, clock);
        return errorLogs.findBetween(range.fromInclusive(), range.toExclusive());
    }
}
