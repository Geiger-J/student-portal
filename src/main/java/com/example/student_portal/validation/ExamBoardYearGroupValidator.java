package com.example.student_portal.validation;

import com.example.student_portal.model.ExamBoard;
import com.example.student_portal.model.YearGroup;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator to ensure exam board compatibility with year group.
 * 
 * Business rules:
 * - Year 9-11: Must be NONE (GCSE years)
 * - Year 12-13: Must be IB or A_LEVELS (sixth form)
 */
public class ExamBoardYearGroupValidator implements ConstraintValidator<ValidExamBoardForYearGroup, Object> {

    @Override
    public void initialize(ValidExamBoardForYearGroup constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true; // Let other validators handle null checks
        }

        YearGroup yearGroup = null;
        ExamBoard examBoard = null;

        // Handle User objects
        if (obj instanceof com.example.student_portal.entity.User) {
            com.example.student_portal.entity.User user = (com.example.student_portal.entity.User) obj;
            yearGroup = user.getYearGroup();
            examBoard = user.getExamBoard();
        }

        if (yearGroup == null || examBoard == null) {
            return true; // Let required field validators handle this
        }

        boolean isValid = switch (yearGroup) {
            case YEAR_9, YEAR_10, YEAR_11 -> examBoard == ExamBoard.NONE;
            case YEAR_12, YEAR_13 -> examBoard == ExamBoard.IB || examBoard == ExamBoard.A_LEVELS;
        };

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            String message = yearGroup.ordinal() <= YearGroup.YEAR_11.ordinal() 
                ? "Years 9-11 must have exam board set to NONE (GCSE years)"
                : "Years 12-13 must select either IB or A-Levels exam board";
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }

        return isValid;
    }
}