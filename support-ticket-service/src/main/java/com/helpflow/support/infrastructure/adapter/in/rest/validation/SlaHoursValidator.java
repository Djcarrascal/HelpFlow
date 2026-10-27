package com.helpflow.support.infrastructure.adapter.in.rest.validation;

import com.helpflow.support.domain.model.Priority;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.SlaUpdateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;

public class SlaHoursValidator implements ConstraintValidator<ValidSlaHours, SlaUpdateRequest> {

    // Tope de horas por prioridad: más urgente, menos tiempo permitido
    private static final Map<Priority, Integer> MAXIMO = Map.of(
            Priority.CRITICA, 8, Priority.ALTA, 24, Priority.MEDIA, 72, Priority.BAJA, 168);

    @Override
    public boolean isValid(SlaUpdateRequest r, ConstraintValidatorContext ctx) {
        if (r == null || r.prioridad() == null || r.horasLimite() == null) return true;
        int maximo = MAXIMO.get(r.prioridad());
        if (r.horasLimite() <= maximo) return true;
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate("para " + r.prioridad() + " el máximo es " + maximo + " horas")
                .addPropertyNode("horasLimite").addConstraintViolation();
        return false;
    }
}
