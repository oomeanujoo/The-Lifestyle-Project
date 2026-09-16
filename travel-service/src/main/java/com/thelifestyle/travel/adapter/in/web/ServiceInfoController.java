package com.thelifestyle.travel.adapter.in.web;

import com.thelifestyle.travel.application.ServiceInfoUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/travel/v1")
public class ServiceInfoController {
    private final ServiceInfoUseCase useCase;
    public ServiceInfoController(ServiceInfoUseCase useCase) { this.useCase = useCase; }
    @GetMapping("/info")
    public ServiceInfoResponse info() {
        var status = useCase.getInfo();
        return new ServiceInfoResponse(status.service(), status.version(), status.status());
    }
}
