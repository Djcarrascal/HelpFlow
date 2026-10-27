package com.helpflow.support.application.port.in;

public record RegisterUserCommand(String documento, String nombre, String correo,
                                  String password, String sede, String area) {}
