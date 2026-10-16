/**
 * Privileges held by an account, mirroring the UserRole enum on the server.
 * The API sends and accepts the bare constant name, not the ROLE_ prefixed authority.
 */
export type UserRole = 'USER' | 'ADMIN';

/**
 * The signed-in user's view of their own account, from /rest/user/me.
 */
export interface UserProfile {
  userAccessId: number;
  accessName: string;
  /** null when the user has not set one, or cleared it. */
  email: string | null;
  enabled: boolean;
  roles: UserRole[];
}

/**
 * An administrator's view of any account, from /rest/admin/users.
 * Carries the tenants the user owns, which the self profile does not.
 */
export interface AdminUserSummary {
  userAccessId: number;
  accessName: string;
  email: string | null;
  enabled: boolean;
  roles: UserRole[];
  tenantNames: string[];
}

export interface CreateUserRequest {
  accessName: string;
  password: string;
  email?: string;
  /** Defaults to [USER] on the server when omitted or empty. */
  roles?: UserRole[];
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/**
 * The error body returned by UserManagementExceptionAdvice.
 */
export interface UserManagementError {
  error: string;
  message: string;
}

/** Minimum accepted by the server, see UserAccessManagementService.MIN_PASSWORD_LENGTH. */
export const MIN_PASSWORD_LENGTH = 8;
