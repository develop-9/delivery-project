package com.delivery_project.slack_service.ai_history.presentation.internal_controller;

import com.delivery_project.slack_service.ai_history.application.command.AiHistoryCreateCommand;
import com.delivery_project.slack_service.ai_history.application.command_service.AiHistoryCommandService;
import com.delivery_project.slack_service.ai_history.application.result.AiHistoryCreateResult;
import com.delivery_project.slack_service.ai_history.presentation.request.AiHistoryCreateRequest;
import com.delivery_project.slack_service.ai_history.presentation.response.AiHistoryCreateResponse;
import com.delivery_project.slack_service.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/ai-histories")
@RequiredArgsConstructor
public class AiHistoryInternalController implements AiHistoryInternal {

    private final AiHistoryCommandService aiHistoryCommandService;

    @Override
    @PostMapping
    public ResponseEntity<SuccessResponse<AiHistoryCreateResponse>> create(
            @Valid @RequestBody
            AiHistoryCreateRequest request
    ) {
        AiHistoryCreateCommand command =
                request.toCommand();

        AiHistoryCreateResult result =
                aiHistoryCommandService.create(
                        command
                );

        AiHistoryCreateResponse response =
                AiHistoryCreateResponse.from(
                        result
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        SuccessResponse.success(response)
                );
    }
}