package com.survivaldiary.domain.policy.service;

import java.util.Arrays;
import java.util.Set;

final class PolicyTargetClassifier {

    private static final String JOB_UNLIMITED = "0013010";

    private PolicyTargetClassifier() {
    }

    static JobMatch classifyJob(String rawJobCodes, String workStatus) {
        if (workStatus == null) {
            return JobMatch.NOT_REQUESTED;
        }

        Set<String> jobCodes = tokens(rawJobCodes);
        if (jobCodes.contains(JOB_UNLIMITED)) {
            return JobMatch.UNRESTRICTED;
        }

        String expectedCode = expectedJobCode(workStatus);
        if (expectedCode == null || jobCodes.isEmpty()) {
            return JobMatch.UNKNOWN;
        }
        return jobCodes.contains(expectedCode) ? JobMatch.MATCHED : JobMatch.MISMATCHED;
    }

    private static String expectedJobCode(String workStatus) {
        return switch (workStatus) {
            case "EMPLOYED" -> "0013001";
            case "SELF_EMPLOYED" -> "0013002";
            case "UNEMPLOYED" -> "0013003";
            case "FREELANCER" -> "0013004";
            case "DAILY_WORKER" -> "0013005";
            case "PROSPECTIVE_FOUNDER" -> "0013006";
            case "SHORT_TERM_WORKER" -> "0013007";
            case "FARMER" -> "0013008";
            case "OTHER" -> "0013009";
            default -> null;
        };
    }

    private static Set<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Set.copyOf(Arrays.stream(value.split("[,|\\s]+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList());
    }

    enum JobMatch {
        NOT_REQUESTED,
        UNKNOWN,
        UNRESTRICTED,
        MATCHED,
        MISMATCHED
    }
}
