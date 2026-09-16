package com.thelifestyle.travel.application.port.out;

import com.thelifestyle.travel.domain.MasterRefreshOutcome;
import com.thelifestyle.travel.domain.MasterStatus;

import java.time.Instant;
import java.util.List;

public interface MasterRefreshLogRepository {
    void record(MasterRefreshOutcome outcome, Instant startedAt);
    List<MasterStatus> statusForAllMasters();
}
