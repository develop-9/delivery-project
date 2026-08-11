package com.delivery_project.delivery_service.delivery.application.port;

import java.util.UUID;

public interface HubPort {

    void validateHubExists(UUID hubId);
}