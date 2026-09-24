package org.immregistries.iis.kernal.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.immregistries.iis.kernal.Application;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End to end cover for the user management API: the admin bootstrap, the ROLE_ADMIN matcher
 * in ServerSecurityConfig, and the self service endpoints.
 * <p>
 * Each request after login carries the session cookie, so the principal is the detached copy
 * restored from the session. That is what would surface a LazyInitializationException if the
 * roles element collection ever stopped being EAGER.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class}, properties =
	{
		"spring.datasource.url=jdbc:h2:mem:db-user-mgmt",
		"spring.batch.job.enabled=false",
		"hapi.fhir.fhir_version=r4",
		"hapi.fhir.cr_enabled=false",
		"hapi.fhir.mdm_enabled=false",
		"springdoc.swagger-ui.enabled=false",
		"hapi.fhir.partitioning.request_tenant_partitioning_mode=true",
		"IIS_MYSQL_URL=jdbc:h2:mem:usersTenantsUserMgmt",
		"ENV_IIS_JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
		"jpa.properties.hibernate.dialect=ca.uhn.fhir.jpa.model.dialect.HapiFhirH2Dialect",
		"hibernate.dialect=ca.uhn.fhir.jpa.model.dialect.HapiFhirH2Dialect",
		"server.servlet.context-path=/iis",

		"iis.deployment.admin.user=bootstrapadmin",
		"iis.deployment.admin.password=Sup3rSecretAdmin",
		"iis.deployment.admin.create-if-not-exists=true",
	})
class UserManagementRestIT {

	private static final String ADMIN_USER = "bootstrapadmin";
	private static final String ADMIN_PASSWORD = "Sup3rSecretAdmin";

	private final TestRestTemplate restTemplate = new TestRestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@LocalServerPort
	private int port;

	private String url(String path) {
		return "http://localhost:" + port + "/iis" + path;
	}

	/**
	 * @return the JSESSIONID cookie, or null when the credentials were rejected.
	 */
	private String login(String username, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		// Makes FormAuthenticationSuccessHandler answer 200 rather than a redirect.
		headers.add("X-Requested-With", "XMLHttpRequest");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("USERID", username);
		form.add("PASSWORD", password);

		ResponseEntity<String> response =
			restTemplate.exchange(url("/login"), HttpMethod.POST, new HttpEntity<>(form, headers), String.class);
		List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
		if (cookies == null) {
			return null;
		}
		return cookies.stream()
			.filter(cookie -> cookie.startsWith("JSESSIONID="))
			.map(cookie -> cookie.split(";", 2)[0])
			.findFirst()
			.orElse(null);
	}

	private ResponseEntity<String> call(HttpMethod method, String path, String sessionCookie, String jsonBody) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		if (sessionCookie != null) {
			headers.add(HttpHeaders.COOKIE, sessionCookie);
		}
		return restTemplate.exchange(url(path), method, new HttpEntity<>(jsonBody, headers), String.class);
	}

	private ResponseEntity<String> get(String path, String sessionCookie) {
		return call(HttpMethod.GET, path, sessionCookie, null);
	}

	@Test
	void anonymousAdminCallIsUnauthorizedRatherThanRedirectedToTheLoginPage() {
		ResponseEntity<String> response = get("/rest/admin/users", null);
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
			"the REST entry point must answer with a status code, not a 302 to /loginForm");
	}

	@Test
	void anonymousSelfCallIsUnauthorized() {
		assertEquals(HttpStatus.UNAUTHORIZED, get("/rest/user/me", null).getStatusCode());
	}

	@Test
	void plainUserIsForbiddenFromTheAdminApi() {
		String cookie = login("plainuser-forbidden", "PlainUserPassword1");
		assertNotNull(cookie, "self registration should have created this user at login");
		assertEquals(HttpStatus.FORBIDDEN, get("/rest/admin/users", cookie).getStatusCode());
	}

	@Test
	void bootstrappedAdminCanListEveryUserWithTheirTenants() throws Exception {
		String cookie = login(ADMIN_USER, ADMIN_PASSWORD);
		assertNotNull(cookie, "the admin bootstrap should have created " + ADMIN_USER);

		ResponseEntity<String> response = get("/rest/admin/users", cookie);
		assertEquals(HttpStatus.OK, response.getStatusCode());

		JsonNode users = objectMapper.readTree(response.getBody());
		assertTrue(users.isArray() && users.size() > 0);
		JsonNode admin = null;
		for (JsonNode user : users) {
			assertFalse(user.has("accessKey"), "the listing must never carry a password hash");
			if (ADMIN_USER.equals(user.path("accessName").asText())) {
				admin = user;
			}
		}
		assertNotNull(admin, "the seeded admin should appear in the listing");
		assertTrue(admin.path("enabled").asBoolean());
		assertTrue(admin.path("roles").toString().contains("ADMIN"));
		assertTrue(admin.has("tenantNames"));
	}

	@Test
	void userCanReadAndUpdateTheirOwnProfile() throws Exception {
		String cookie = login("profile-owner", "ProfileOwnerPass1");
		assertNotNull(cookie);

		JsonNode profile = objectMapper.readTree(get("/rest/user/me", cookie).getBody());
		assertEquals("profile-owner", profile.path("accessName").asText());
		assertTrue(profile.path("email").isNull());

		ResponseEntity<String> updated = call(HttpMethod.PUT, "/rest/user/me/email", cookie,
			"{\"email\":\"Profile.Owner@Example.ORG\"}");
		assertEquals(HttpStatus.OK, updated.getStatusCode());
		assertEquals("profile.owner@example.org", objectMapper.readTree(updated.getBody()).path("email").asText(),
			"email should be normalised");

		assertEquals(HttpStatus.BAD_REQUEST,
			call(HttpMethod.PUT, "/rest/user/me/email", cookie, "{\"email\":\"not-an-email\"}").getStatusCode());
	}

	@Test
	void changingOwnPasswordRequiresTheCurrentOneAndTakesEffect() {
		String cookie = login("password-changer", "OriginalPassword1");
		assertNotNull(cookie);

		assertEquals(HttpStatus.UNAUTHORIZED,
			call(HttpMethod.POST, "/rest/user/me/password", cookie,
				"{\"currentPassword\":\"WrongPassword1\",\"newPassword\":\"ReplacementPass1\"}").getStatusCode());

		assertEquals(HttpStatus.BAD_REQUEST,
			call(HttpMethod.POST, "/rest/user/me/password", cookie,
				"{\"currentPassword\":\"OriginalPassword1\",\"newPassword\":\"short\"}").getStatusCode(),
			"a too short password should be rejected");

		assertEquals(HttpStatus.NO_CONTENT,
			call(HttpMethod.POST, "/rest/user/me/password", cookie,
				"{\"currentPassword\":\"OriginalPassword1\",\"newPassword\":\"ReplacementPass1\"}").getStatusCode());

		assertNotNull(login("password-changer", "ReplacementPass1"), "the new password should work");
	}

	@Test
	void adminCanDisableAUserWhoCanThenNoLongerSignIn() throws Exception {
		String victimCookie = login("to-be-disabled", "ToBeDisabledPass1");
		assertNotNull(victimCookie);

		String adminCookie = login(ADMIN_USER, ADMIN_PASSWORD);
		assertNotNull(adminCookie);
		int victimId = findUserId(adminCookie, "to-be-disabled");

		ResponseEntity<String> disabled = call(HttpMethod.PUT, "/rest/admin/users/" + victimId + "/enabled",
			adminCookie, "{\"enabled\":false}");
		assertEquals(HttpStatus.OK, disabled.getStatusCode());
		assertFalse(objectMapper.readTree(disabled.getBody()).path("enabled").asBoolean());

		assertNull(login("to-be-disabled", "ToBeDisabledPass1"), "a disabled user must not be able to sign in");

		/*
		 * The session opened before the disable is still valid as far as the servlet
		 * container is concerned, but the service reloads the row, so self service is closed.
		 */
		assertEquals(HttpStatus.UNAUTHORIZED, get("/rest/user/me", victimCookie).getStatusCode());
	}

	@Test
	void adminCannotDisableTheLastRemainingAdmin() {
		String adminCookie = login(ADMIN_USER, ADMIN_PASSWORD);
		assertNotNull(adminCookie);
		int adminId = findUserId(adminCookie, ADMIN_USER);

		assertEquals(HttpStatus.BAD_REQUEST,
			call(HttpMethod.PUT, "/rest/admin/users/" + adminId + "/enabled", adminCookie, "{\"enabled\":false}")
				.getStatusCode(),
			"disabling the only admin would lock everyone out");
	}

	@Test
	void adminCanCreateAUserWhoCanThenSignIn() throws Exception {
		String adminCookie = login(ADMIN_USER, ADMIN_PASSWORD);
		assertNotNull(adminCookie);

		ResponseEntity<String> created = call(HttpMethod.POST, "/rest/admin/users", adminCookie,
			"{\"accessName\":\"admin-created\",\"password\":\"AdminCreatedPass1\",\"email\":\"created@example.org\"}");
		assertEquals(HttpStatus.OK, created.getStatusCode());
		assertEquals("created@example.org", objectMapper.readTree(created.getBody()).path("email").asText());

		assertNotNull(login("admin-created", "AdminCreatedPass1"));

		assertEquals(HttpStatus.BAD_REQUEST,
			call(HttpMethod.POST, "/rest/admin/users", adminCookie,
				"{\"accessName\":\"admin-created\",\"password\":\"AnotherPassword1\"}").getStatusCode(),
			"duplicate accessName should be rejected");
	}

	@Test
	void unknownUserIdIsNotFound() {
		String adminCookie = login(ADMIN_USER, ADMIN_PASSWORD);
		assertEquals(HttpStatus.NOT_FOUND, get("/rest/admin/users/999999", adminCookie).getStatusCode());
	}

	private int findUserId(String adminCookie, String accessName) {
		try {
			JsonNode users = objectMapper.readTree(get("/rest/admin/users", adminCookie).getBody());
			for (JsonNode user : users) {
				if (accessName.equals(user.path("accessName").asText())) {
					return user.path("userAccessId").asInt();
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		throw new IllegalStateException("No user named " + accessName + " in the admin listing");
	}

	private static void assertNull(Object value, String message) {
		org.junit.jupiter.api.Assertions.assertNull(value, message);
	}
}
