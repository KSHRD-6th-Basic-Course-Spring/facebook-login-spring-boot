package com.chhaileng.app.model;

import org.springframework.security.core.GrantedAuthority;

public class Role implements GrantedAuthority {
	private static final long serialVersionUID = 1L;
	
	private int id;
	private String role;

	public Role() {
		this.id = 1;
		this.role = "ROLE_FACEBOOK_USER";
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public String toString() {
		return "Role [id=" + id + ", role=" + role + "]";
	}

	@Override
	public String getAuthority() {
		return "ROLE_" + role;
	}

}
