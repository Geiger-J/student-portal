package com.example.student_portal.model;

/**
 * Enum for exam boards used in sixth form.
 *
 * For Year 12 and Year 13 students, an exam board is required.
 * For other years, this can be NONE.
 */
public enum ExamBoard {
    IB,        // International Baccalaureate
    A_LEVELS,  // A-Levels
    NONE       // Not applicable (for years below sixth form)
}