package com.thelifestyle.integration.application;

import com.thelifestyle.integration.domain.ServiceStatus;
import org.springframework.stereotype.Service;

@Service
public class ServiceInfoUseCase {
    public ServiceStatus getInfo() { return new ServiceStatus("integration-service", "0.1.0", "operational"); }
}
