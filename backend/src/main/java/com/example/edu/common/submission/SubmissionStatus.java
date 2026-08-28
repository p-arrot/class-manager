package com.example.edu.common.submission;

import java.util.Set;

public final class SubmissionStatus {
    public static final String IN_PROGRESS = "in_progress";
    public static final String SUBMITTED = "submitted";
    public static final String GRADED = "graded";
    public static final String RETURNED = "returned";
    public static final String ABSENT = "absent";
    public static final String SPECIAL = "special";

    private static final Set<String> LOCKED = Set.of(GRADED, ABSENT, SPECIAL);

    private SubmissionStatus() {}

    public static boolean isLocked(String status) {
        return status != null && LOCKED.contains(status);
    }

    public static boolean canResubmit(String status) {
        return status == null || SUBMITTED.equals(status) || RETURNED.equals(status) || IN_PROGRESS.equals(status);
    }
}
