package com.helpflow.support.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                                   SecurityProblemHandler problemHandler) throws Exception {
        http
            .csrf(csrf -> csrf.disable())     // API stateless con tokens, sin cookies de sesión
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // públicos (/error: si no, todo error interno sale como 401)
                .requestMatchers("/auth/**", "/error").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // observabilidad y configuración: solo admin
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/sla/**").hasRole("ADMIN")
                // tickets
                .requestMatchers(HttpMethod.POST, "/api/tickets").hasAnyRole("USUARIO", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/estado").hasAnyRole("TECNICO", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/reasignar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/tickets/*/asignar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/tickets/escalamiento").hasRole("ADMIN")
                // el resto (consultas, perfil) solo requiere estar autenticado
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(problemHandler)
                .accessDeniedHandler(problemHandler))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // BCrypt: hash con sal aleatoria, lento a propósito contra fuerza bruta
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
