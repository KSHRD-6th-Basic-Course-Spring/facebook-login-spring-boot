package com.chhaileng.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

	@GetMapping("/login")
	public String login(){
		return "login";
	}
	
	@GetMapping(value = {"/index"})
	public String home() {
		return "index";
	}
	
}
