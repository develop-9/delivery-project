package com.delivery_project.company_service.company.application.port;

import com.delivery_project.company_service.company.application.port.dto.HubInfo;

import java.util.UUID;

public interface HubPort {

    HubInfo validateHub(UUID hubId);
}
