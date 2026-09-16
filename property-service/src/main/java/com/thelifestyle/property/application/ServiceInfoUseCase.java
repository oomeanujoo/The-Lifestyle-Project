package com.thelifestyle.property.application;

import com.thelifestyle.property.domain.ServiceStatus;
import org.springframework.stereotype.Service;

@Service
public class ServiceInfoUseCase {
    public ServiceStatus getInfo() { return new ServiceStatus("property-service", "0.1.0", "operational"); }
}
