package com.delivery_project.user_service.user.presentation.api_controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery_project.user_service.global.response.SuccessResponse;
import com.delivery_project.user_service.user.application.command_service.AuthCommandService;
import com.delivery_project.user_service.user.application.result.UserLoginResult;
import com.delivery_project.user_service.user.application.result.UserRefreshResult;
import com.delivery_project.user_service.user.application.result.UserSignupResult;
import com.delivery_project.user_service.user.presentation.request.UserLoginRequest;
import com.delivery_project.user_service.user.presentation.request.UserRefreshRequest;
import com.delivery_project.user_service.user.presentation.request.UserSignupRequest;
import com.delivery_project.user_service.user.presentation.response.UserLoginResponse;
import com.delivery_project.user_service.user.presentation.response.UserRefreshResponse;
import com.delivery_project.user_service.user.presentation.response.UserSignupResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

	private final AuthCommandService authCommandService;

	@PostMapping("/signup")
	public ResponseEntity<SuccessResponse<UserSignupResponse>> signup(@Valid @RequestBody UserSignupRequest request) {
		UserSignupResult result = authCommandService.signup(request.toCommand());
		UserSignupResponse response = UserSignupResponse.from(result);
		return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
	}

	@PostMapping("/login")
	public ResponseEntity<SuccessResponse<UserLoginResponse>> login(@Valid @RequestBody UserLoginRequest request) {
		UserLoginResult result = authCommandService.login(request.toCommand());
		UserLoginResponse response = UserLoginResponse.from(result);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@PostMapping("/refresh")
	public ResponseEntity<SuccessResponse<UserRefreshResponse>> refresh(@Valid @RequestBody UserRefreshRequest request) {
		UserRefreshResult result = authCommandService.refresh(request.toCommand());
		UserRefreshResponse response = UserRefreshResponse.from(result);
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
	) {
		authCommandService.logout(authorization);
		return ResponseEntity.noContent().build();
	}
}
