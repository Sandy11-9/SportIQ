package com.sportiq.backend;

import com.sportiq.backend.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests extends PostgresTestBase {

	@Test
	void contextLoads() {
	}
}