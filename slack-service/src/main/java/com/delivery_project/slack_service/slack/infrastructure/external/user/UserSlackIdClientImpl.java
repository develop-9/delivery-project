package com.delivery_project.slack_service.slack.infrastructure.external.user;

import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import com.delivery_project.slack_service.slack.application.port.UserSlackIdClient;
import com.delivery_project.slack_service.slack.application.result.UserSlackIdResult;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSlackIdClientImpl implements UserSlackIdClient {

    private final UserSlackIdFeignClient userSlackIdFeignClient;

    @Override
    public UserSlackIdResult getUserSlackId(UUID userId) {
        try {
            UserSlackIdFeignResponse response =
                    userSlackIdFeignClient
                            .getUserSlackId(userId)
                            .data();

            if (response == null) {
                throw new BusinessException(
                        ErrorCode.USER_SLACK_ID_NOT_FOUND
                );
            }

            return response.toResult();

        } catch (FeignException.NotFound exception) {
            throw new BusinessException(
                    ErrorCode.USER_SLACK_ID_NOT_FOUND
            );

        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
            );
        }
    }
}