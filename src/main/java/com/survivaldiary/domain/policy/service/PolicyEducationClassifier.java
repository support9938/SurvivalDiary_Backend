package com.survivaldiary.domain.policy.service;

import java.util.Arrays;
import java.util.Set;

final class PolicyEducationClassifier {

    private static final String UNRESTRICTED = "0049010";

    private PolicyEducationClassifier() {
    }

    static EducationMatch classify(
            String schoolCodes,
            String educationLevel,
            String enrollmentStatus
    ) {
        if (educationLevel == null && enrollmentStatus == null) {
            return EducationMatch.NOT_REQUESTED;
        }
        if (schoolCodes == null || schoolCodes.isBlank()) {
            return EducationMatch.UNRESTRICTED;
        }

        Set<String> codes = Arrays.stream(schoolCodes.split("[,|\\s]+"))
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .collect(java.util.stream.Collectors.toSet());
        if (codes.contains(UNRESTRICTED)) {
            return EducationMatch.UNRESTRICTED;
        }

        String expectedCode = expectedCode(educationLevel, enrollmentStatus);
        if (expectedCode == null) {
            return EducationMatch.UNKNOWN;
        }
        return codes.contains(expectedCode)
                ? EducationMatch.MATCHED
                : EducationMatch.MISMATCHED;
    }

    private static String expectedCode(String educationLevel, String enrollmentStatus) {
        if (educationLevel == null) {
            return null;
        }
        return switch (educationLevel) {
            case "MIDDLE_SCHOOL_OR_LESS" -> "0049001";
            case "HIGH_SCHOOL" -> highSchoolCode(enrollmentStatus);
            case "COLLEGE_2_3_YEAR", "UNIVERSITY_4_YEAR" -> collegeCode(enrollmentStatus);
            case "GRADUATE_SCHOOL" -> "0049008";
            case "OTHER" -> "0049009";
            default -> null;
        };
    }

    private static String highSchoolCode(String enrollmentStatus) {
        if (enrollmentStatus == null) {
            return null;
        }
        return switch (enrollmentStatus) {
            case "ENROLLED" -> "0049002";
            case "EXPECTED_GRADUATION" -> "0049003";
            case "GRADUATED" -> "0049004";
            case "DROPPED_OUT" -> "0049001";
            default -> null;
        };
    }

    private static String collegeCode(String enrollmentStatus) {
        if (enrollmentStatus == null) {
            return null;
        }
        return switch (enrollmentStatus) {
            case "ENROLLED" -> "0049005";
            case "EXPECTED_GRADUATION" -> "0049006";
            case "GRADUATED" -> "0049007";
            case "DROPPED_OUT" -> "0049004";
            default -> null;
        };
    }

    enum EducationMatch {
        NOT_REQUESTED,
        UNRESTRICTED,
        MATCHED,
        MISMATCHED,
        UNKNOWN
    }
}
