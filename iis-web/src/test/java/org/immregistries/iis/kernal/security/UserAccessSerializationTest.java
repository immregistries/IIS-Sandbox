package org.immregistries.iis.kernal.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.immregistries.iis.kernal.enums.UserRole;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthenticationRestController hands the principal to AuthInfo as a plain Object, so Jackson
 * serializes this entity's runtime type on GET /rest/authentication, which is permitAll.
 * Nothing sensitive may appear there.
 */
class UserAccessSerializationTest {

	@Test
	void serializedUserAccessLeaksNoCredentialOrProfileField() throws Exception {
		UserAccess userAccess = new UserAccess();
		userAccess.setUserAccessId(3);
		userAccess.setAccessName("Mercy");
		userAccess.setAccessKey("$2a$05$notarealhashbutlongenoughtospot");
		userAccess.setEmail("mercy@example.org");
		userAccess.setEnabled(Boolean.FALSE);
		userAccess.setRoles(new HashSet<>(List.of(UserRole.ADMIN)));

		String json = new ObjectMapper().writeValueAsString(userAccess);

		for (String forbidden : List.of("accessKey", "notarealhash", "email", "mercy@example.org", "roles", "ADMIN",
			"enabled", "authorities", "credentials")) {
			assertFalse(json.contains(forbidden), "serialized UserAccess must not contain " + forbidden + " : " + json);
		}
		assertTrue(json.contains("Mercy"), "accessName is the one identifying field callers already rely on");
	}
}
