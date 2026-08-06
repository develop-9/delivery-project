package com.delivery_project.delivery_service.delivery.infrastructure.adapter;

import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.result.ReceiverInfo;
import com.delivery_project.delivery_service.delivery.infrastructure.client.UserInternalClient;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.UserInfoResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.UserSlackResponse;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserFeignAdapter implements UserPort {
    private final UserInternalClient userInternalClient;
    /*
     * User Service 호출
     * Slack ID 호출
     * Feign DTO 해제
     * Application 객체로 변환
     * FeignException을 BusinessException으로 변환
     */
    @Override
    public ReceiverInfo getReceiver(
            String authorization,
            UUID receiverUserId
    ){
        try {
            InternalApiResponse<UserInfoResponse> userResponse =
                    userInternalClient.getUser(
                            authorization,
                            receiverUserId
                    );

            InternalApiResponse<UserSlackResponse> slackResponse =
                    userInternalClient.getUserSlack(
                            authorization,
                            receiverUserId
                    );
            UserInfoResponse user = userResponse.data();
            UserSlackResponse slack = slackResponse.data();

            return new ReceiverInfo(
                    user.userId(),
                    user.name(),
                    slack.slackId()
            );
        } catch (FeignException.NotFound exception){
            throw new BusinessException(
                    ErrorCode.USER_NOT_FOUND
            );
        } catch (FeignException exception){
            /*
             * USER_SERVICE_UNAVAILABLE을 추가할지,
             * 공통 INTERNAL_SERVER_ERROR를 사용할지는
             * 팀의 Feign 예외 변환 규칙을 확인한 뒤 확정한다.
             */
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }
}
