package com.example.it_asset_tracker;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ItAssetTrackerApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void unauthenticatedRequestHasStructuredErrorResponse() throws Exception {
		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.path").value("/api/categories"));
	}

	@Test
	void forbiddenRequestHasStructuredErrorResponse() throws Exception {
		mockMvc.perform(post("/api/categories")
						.with(jwt().authorities(
								new SimpleGrantedAuthority("ROLE_STAFF"))))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.path").value("/api/categories"));
	}

	@Test
	void invalidAssetStatusHasStructuredBadRequestResponse() throws Exception {
		mockMvc.perform(get("/api/assets")
						.param("status", "BROKEN")
						.with(jwt().authorities(
								new SimpleGrantedAuthority("ROLE_STAFF"))))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value("/api/assets"));
	}

	@Test
	void unknownEndpointHasStructuredNotFoundResponse() throws Exception {
		mockMvc.perform(get("/api/not-a-real-endpoint")
						.with(jwt().authorities(
								new SimpleGrantedAuthority("ROLE_STAFF"))))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.path").value("/api/not-a-real-endpoint"));
	}

	@Test
	void registrationValidatesTrimmedUsername() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType("application/json")
						.content("""
								{"username":" ab ","password":"long-enough-password"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.username")
						.value("Username must be between 3 and 50 characters"));
	}
}
