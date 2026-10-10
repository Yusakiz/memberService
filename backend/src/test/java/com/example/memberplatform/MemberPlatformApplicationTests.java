package com.example.memberplatform;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class MemberPlatformApplicationTests {

	private static final Path TEST_DATABASE = Path.of(
			"target", "test-data", "context-" + UUID.randomUUID() + ".db");

	@Autowired
	JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("app.database.path", TEST_DATABASE::toString);
	}

	@Test
	void createsMembersTableOnStartup() {
		Integer tableCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'members'",
				Integer.class);
		assertThat(tableCount).isEqualTo(1);
	}

}
