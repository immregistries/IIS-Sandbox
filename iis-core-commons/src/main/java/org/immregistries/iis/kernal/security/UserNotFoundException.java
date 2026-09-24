package org.immregistries.iis.kernal.security;

/**
 * Raised when a user management operation targets an id that does not exist.
 */
public class UserNotFoundException extends IllegalArgumentException {

	public UserNotFoundException(int userAccessId) {
		super("No user with id " + userAccessId);
	}
}
