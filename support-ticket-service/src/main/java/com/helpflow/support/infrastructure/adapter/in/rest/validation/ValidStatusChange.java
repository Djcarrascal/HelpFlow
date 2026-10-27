package com.helpflow.support.infrastructure.adapter.in.rest.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StatusChangeValidator.class)
public @interface ValidStatusChange {
    String message() default "Cambio de estado inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
