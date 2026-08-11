package com.delivery_project.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 주문·재고 서비스.
 *
 * <p>company(수령 업체 조회)·delivery(배송 생성·취소) 연동을 위해 Feign 을 켠다.
 * 외부 호출은 모두 {@code order.application.port} 뒤에 두고
 * {@code order.infrastructure.adapter} 에서 구현한다.
 */
@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

}
