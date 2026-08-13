package com.delivery_project.hub_service.hub.application.support;

/**
 * 허브 캐시 이름 (00_common.md).
 *
 * <p><b>이름은 application 이 가진다</b> — {@code @Cacheable} 을 다는 쪽이 여기이기 때문이다.
 * 그 값을 어떤 직렬화기로 얼마나 담아둘지는 {@code hub/infrastructure/config/HubCacheConfig} 가,
 * 무효화는 {@code hub/infrastructure/adapter/HubCacheAdapter} 가 정한다.
 * 이름을 infrastructure 에 두면 application 이 바깥을 import 하게 된다.
 */
public final class HubCacheNames {

	/** 허브 단건. 키는 {@code hub:{hubId}}. */
	public static final String HUB = "hub";

	/** 산출된 경로. 키는 {@code hubPath:{depId}:{arrId}}. */
	public static final String HUB_PATH = "hubPath";

	private HubCacheNames() {
	}
}
