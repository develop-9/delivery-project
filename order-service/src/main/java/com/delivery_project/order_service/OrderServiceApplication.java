package com.delivery_project.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 1단계에서는 주문 CRUD + Search 만 제공하며 다른 서비스를 호출하지 않는다.
 * company / hub / delivery 연동을 붙일 때 @EnableFeignClients 를 다시 켠다.
 */
@SpringBootApplication
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

}
