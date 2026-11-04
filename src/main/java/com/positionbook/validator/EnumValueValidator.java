package com.positionbook.validator;

import com.positionbook.annotation.EnumValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

    private List<String> acceptedValues;

    @Override
    public void initialize(final EnumValue annotation) {
        acceptedValues = Stream.of(annotation.enumType().getEnumConstants()).map(Enum::name)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        return null == value || value.isBlank() || acceptedValues.contains(value);
    }
}
