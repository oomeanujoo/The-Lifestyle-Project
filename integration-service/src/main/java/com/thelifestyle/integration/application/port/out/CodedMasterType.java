package com.thelifestyle.integration.application.port.out;

import java.util.Arrays;
import java.util.Optional;

// transport_mode, bhk_type, and service_addon are all the same shape
// (code UNIQUE, label) — one generic repository (§10) parameterized by
// this enum instead of three near-identical classes. The enum, not a raw
// String, is what keeps CodedMasterRepository's SQL injection-proof by
// construction — table() only ever returns one of these three literals.
public enum CodedMasterType {
    TRANSPORT_MODE("transport_mode", "lifestyle_master.transport_mode"),
    BHK_TYPE("bhk_type", "lifestyle_master.bhk_type"),
    SERVICE_ADDON("service_addon", "lifestyle_master.service_addon");

    private final String masterName;
    private final String table;

    CodedMasterType(String masterName, String table) {
        this.masterName = masterName;
        this.table = table;
    }

    public String masterName() {
        return masterName;
    }

    public String table() {
        return table;
    }

    // For anything holding a master_proposal's plain-String masterType
    // (MasterAcquisitionUseCase, MasterProposalUseCase's accept-and-apply
    // step) that needs to know whether it names one of these three
    // flat, FK-free coded masters — never trusts a raw String as this enum
    // itself elsewhere.
    public static Optional<CodedMasterType> fromMasterName(String masterName) {
        return Arrays.stream(values()).filter(t -> t.masterName.equals(masterName)).findFirst();
    }
}
