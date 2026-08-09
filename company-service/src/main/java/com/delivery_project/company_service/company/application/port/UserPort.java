package com.delivery_project.company_service.company.application.port;

import com.delivery_project.company_service.company.application.port.dto.CallerInfo;

import java.util.UUID;

public interface UserPort {

     CallerInfo getCaller(UUID callerId);
}
