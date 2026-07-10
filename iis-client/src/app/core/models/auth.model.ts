export interface AuthInfo {
  authenticated: boolean;
  name: string;
  principal?: unknown;
  authorities?: { authority: string }[];
}
