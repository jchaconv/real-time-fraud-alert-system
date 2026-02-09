package com.jchacon.banking.frauddetection.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueInEnumValidator implements ConstraintValidator<ValueInEnum, String> {
    private List<String> acceptedValues;

    @Override
    public void initialize(ValueInEnum annotation) {
        // Get all enum constants and store them as a list of strings
        acceptedValues = Stream.of(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Use @NotBlank or @NotNull to handle nulls
        }
        return acceptedValues.contains(value.toUpperCase());
    }
}