package org.immregistries.iis.kernal.model.persisted;

/**
 * Persisting generated key for smart health links and cards
 */
public class IisShlinkContent {

	private int id;
	private UserAccess userAccess;
	private String content;
	private Long exp;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public UserAccess getUserAccess() {
		return userAccess;
	}

	public void setUserAccess(UserAccess userAccess) {
		this.userAccess = userAccess;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Long getExp() {
		return exp;
	}

	public void setExp(Long exp) {
		this.exp = exp;
	}
}
