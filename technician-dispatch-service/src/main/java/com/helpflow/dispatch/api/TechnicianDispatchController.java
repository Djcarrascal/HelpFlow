package com.helpflow.dispatch.api;

import com.helpflow.dispatch.service.TechnicianDispatchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/technician-dispatch")
public class TechnicianDispatchController {

    private final TechnicianDispatchService service;

    public TechnicianDispatchController(TechnicianDispatchService service) {
        this.service = service;
    }

    // @Valid dispara las validaciones del record antes de entrar al método
    @PostMapping
    public DispatchResponse dispatch(@Valid @RequestBody DispatchRequest request) {
        return service.dispatch(request);
    }
}
