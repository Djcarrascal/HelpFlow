package com.helpflow.support.domain.model;

public enum Priority {
    BAJA, MEDIA, ALTA, CRITICA;

    // Prioridad inmediatamente superior; CRITICA es el tope
    public Priority siguiente() {
        return this == CRITICA ? CRITICA : values()[ordinal() + 1];
    }
}
