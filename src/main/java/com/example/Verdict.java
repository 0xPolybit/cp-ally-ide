package com.example;

/**
 * Verdict for a single test-case result. Replaces the previous
 * (passed, timedOut, unknown) triple with a single enum that
 * carries all the information the UI needs to label a result
 * accurately.
 */
enum Verdict {
    /** Output matched the expected output. */
    PASSED,
    /** Output did not match. */
    WRONG_ANSWER,
    /** Process exited with a non-zero status. */
    RUNTIME_ERROR,
    /** Process exceeded the time limit. */
    TIME_LIMIT_EXCEEDED,
    /** Process produced more output than the configured cap. */
    OUTPUT_LIMIT_EXCEEDED,
    /** The user pressed Stop. */
    CANCELED,
    /** Sample test had no expected output. */
    UNKNOWN_NO_EXPECTED_OUTPUT
}
