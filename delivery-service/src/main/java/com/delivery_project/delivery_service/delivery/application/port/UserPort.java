package com.delivery_project.delivery_service.delivery.application.port;

import com.delivery_project.delivery_service.delivery.application.result.ReceiverInfo;
import com.delivery_project.delivery_service.delivery.application.result.UserAuthorizationInfo;

import java.util.UUID;

public interface UserPort {

    ReceiverInfo getReceiver(
            UUID receiverUserId
    );

    UserAuthorizationInfo getUserAuthorizationInfo(
            UUID userId
    );
}
