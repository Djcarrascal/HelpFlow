package com.helpflow.support.infrastructure.error;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.OffsetDateTime;

// Formato RFC 7807 con timestamp y traceId como propiedades extra
public final class ProblemDetails {

    private static final String BASE_TYPE = "https://api.helpflow.com/errors/";

    private ProblemDetails() {}

    public static ProblemDetail of(HttpStatus status, String code, String title, String detail, String path) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(BASE_TYPE + code));
        pd.setTitle(title);
        pd.setInstance(URI.create(path));
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        pd.setProperty("traceId", MDC.get(TraceIdFilter.TRACE_ID));
        return pd;
    }
}
