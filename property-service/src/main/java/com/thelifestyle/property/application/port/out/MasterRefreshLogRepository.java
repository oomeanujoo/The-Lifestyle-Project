package com.thelifestyle.property.application.port.out;

import com.thelifestyle.property.domain.MasterRefreshOutcome;
import com.thelifestyle.property.domain.MasterStatus;

import java.time.Instant;
import java.util.List;

public interface MasterRefreshLogRepository {
    void record(MasterRefreshOutcome outcome, Instant startedAt);
    List<MasterStatus> statusForAllMasters();
}
