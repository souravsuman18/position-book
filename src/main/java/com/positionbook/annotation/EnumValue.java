package com.positionbook.annotation;

import com.positionbook.validator.EnumValueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EnumValueValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValue {

    Class<? extends Enum<?>> enumType();

    String message() default "Entered value is not valid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}