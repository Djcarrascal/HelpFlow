package com.helpflow.support.infrastructure.adapter.in.rest.validation;

import com.helpflow.support.domain.model.TicketStatus;
import com.helpflow.support.infrastructure.adapter.in.rest.dto.ChangeStatusRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StatusChangeValidator implements ConstraintValidator<ValidStatusChange, ChangeStatusRequest> {

    @Override
    public boolean isValid(ChangeStatusRequest r, ConstraintValidatorContext ctx) {
        if (r == null || r.estado() == null) return true;    // @NotNull reporta el nulo
        if (r.estado() == TicketStatus.ASIGNADO) {
            return error(ctx, "estado", "ASIGNADO se obtiene con /asignar o /reasignar");
        }
        boolean requiereObservacion = r.estado() == TicketStatus.CANCELADO || r.estado() == TicketStatus.RESUELTO;
        if (requiereObservacion && (r.observacion() == null || r.observacion().isBlank())) {
            return error(ctx, "observacion", "es obligatoria al pasar a " + r.estado());
        }
        return true;
    }

    // Asocia el error al campo concreto en lugar de al objeto completo
    private boolean error(ConstraintValidatorContext ctx, String campo, String mensaje) {
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(mensaje).addPropertyNode(campo).addConstraintViolation();
        return false;
    }
}
