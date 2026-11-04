package com.example.veylora;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VeyloraApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void csrfEndpointReturnsTokenAndSetsCookie() throws Exception {
		mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(cookie().exists("XSRF-TOKEN"));
	}

	@Test
	void unauthenticatedApiRequestsReturnUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/contacts").param("userId", "1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedSessionCanAccessProtectedGetEndpoints() throws Exception {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
				"test@example.com", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER")));
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(
				HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

		mockMvc.perform(get("/api/v1/call-logs").param("userId", "1").session(session))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/workspace-data")
				.param("userId", "1")
				.session(session))
				.andExpect(status().isOk());
	}
}
