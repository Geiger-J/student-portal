package com.example.student_portal.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation to ensure exam board is compatible with year group.
 * 
 * Applied at class level to validate the relationship between yearGroup and examBoard fields.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExamBoardYearGroupValidator.class)
@Documented
public @interface ValidExamBoardForYearGroup {
    
    String message() default "Exam board must be compatible with year group";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}