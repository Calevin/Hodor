package com.calevin.hodor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
		"hodor.admin.username=test-admin",
		"hodor.admin.password=test-pass",
		"hodor.admin.client-id=test-client-id",
		"hodor.admin.client-secret=test-client-secret",
		"hodor.auth.issuer-url=https://hodor-java-auth.calevin.com"
})
@Testcontainers
class HodorApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

	@Test
	void contextLoads() {
	}

}
