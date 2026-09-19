package com.thelifestyle.integration.application.port.out;

import com.thelifestyle.integration.domain.MasterRefreshOutcome;
import com.thelifestyle.integration.domain.MasterStatus;

import java.time.Instant;
import java.util.List;

public interface MasterRefreshLogRepository {
    void record(MasterRefreshOutcome outcome, Instant startedAt);
    List<MasterStatus> statusForAllMasters();
}
