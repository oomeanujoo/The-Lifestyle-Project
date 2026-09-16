package com.thelifestyle.travel.application;

import com.thelifestyle.travel.domain.ServiceStatus;
import org.springframework.stereotype.Service;

@Service
public class ServiceInfoUseCase {
    public ServiceStatus getInfo() { return new ServiceStatus("travel-service", "0.1.0", "operational"); }
}
