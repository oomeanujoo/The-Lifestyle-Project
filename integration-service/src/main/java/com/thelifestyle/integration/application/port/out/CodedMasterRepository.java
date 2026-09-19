package com.thelifestyle.integration.application.port.out;

import com.thelifestyle.integration.domain.CodedMaster;

import java.util.List;

// One generic repository for transport_mode/bhk_type/service_addon (§10) —
// all three are the exact same (code UNIQUE, label) shape in
// lifestyle_master. Upsert-and-append only (§17.1). upsert() returns the
// persisted row (with its id) — these masters are managed purely through
// this method now (MasterDataRefreshUseCase.createOrUpdateCoded()), never
// seeded from a hardcoded Java value, so the caller needs the real
// resulting row back, the same as LifestyleMasterCityRepository.upsert().
public interface CodedMasterRepository {
    CodedMaster upsert(CodedMasterType type, String code, String label);
    List<CodedMaster> findAll(CodedMasterType type);
    long count(CodedMasterType type);
}
