package com.chhaileng.app.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.chhaileng.app.model.Role;
import com.chhaileng.app.model.User;
import com.github.scribejava.apis.FacebookApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

@Controller
@RequestMapping("/facebook")
@PropertySource("classpath:fblogin.properties")
public class FacebookController {

	@Value("${fb.login.appid}")
	private String FB_APP_ID;
	
	@Value("${fb.login.secret}")
	private String FB_APP_SECRET;
	
	@Value("${fb.login.domain}")
	private String DOMAIN;
	
	private static final String CALLBACK_URL = "/facebook/callback";

	private static final List<String> SCOPES = new ArrayList<String>() {
		private static final long serialVersionUID = 1L;
		{
			add("public_profile");
			add("email");
			add("user_gender");
		}
	};

	// Facebook API get user information
	private static final String USER_PROFILE_API_URL = "https://graph.facebook.com/v2.8/me"
												 	 + "?fields=id,name,first_name,last_name,gender,email";

	@GetMapping("/signin")
	public void signin(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		String secretState = "secret" + new Random().nextInt(999_999);
		request.getSession().setAttribute("SECRET_STATE", secretState);
		
		
		@SuppressWarnings("deprecation")
		OAuth20Service service = new ServiceBuilder()
									.apiKey(FB_APP_ID)
									.apiSecret(FB_APP_SECRET)
									.callback(DOMAIN + CALLBACK_URL)
									.scope(String.join(",", SCOPES))
									.state(secretState)
									.build(FacebookApi.instance());

		String authorizeUrl = service.getAuthorizationUrl();
		System.out.println("Authorize URL: " + authorizeUrl);
		response.sendRedirect(authorizeUrl);
	}

	@GetMapping(value = "/callback")
	public String callback(@RequestParam(value = "code", required = false) String code,
						   @RequestParam(value = "state", required = false) String state,
						   HttpServletRequest request,
						   HttpServletResponse response) {

		try {
//			String secretState = (String) request.getSession().getAttribute("SECRET_STATE");
//			
//			if (secretState.equals(state)) {
//				System.out.println("State value match");
//			} else {
//				System.out.println("State value does not match");
//			}

			@SuppressWarnings("deprecation")
			OAuth20Service service = new ServiceBuilder()
										.apiKey(FB_APP_ID)
										.apiSecret(FB_APP_SECRET)
										.callback(DOMAIN + CALLBACK_URL)
										.build(FacebookApi.instance());

			final String requestUrl = USER_PROFILE_API_URL;
			final OAuth2AccessToken accessToken = service.getAccessToken(code);
			final OAuthRequest oauthRequest = new OAuthRequest(Verb.GET, requestUrl);
			service.signRequest(accessToken, oauthRequest);
			
			final Response resourceResponse = service.execute(oauthRequest);

			System.out.println("RESPONSE CODE: " + resourceResponse.getCode() + ", MESSAGE: " + resourceResponse.getMessage());
			System.out.println("BODY: " + resourceResponse.getBody());

			final JSONObject obj = new JSONObject(resourceResponse.getBody());
			System.out.println("JSON BODY: " + obj.toString());

			request.getSession().setAttribute("FACEBOOK_ACCESS_TOKEN", accessToken);

			// Sign up new user and create login session
			User user = new User();
			user.setId(1);
			
			// Try to set email if user allow email access
			try { user.setEmail(obj.getString("email")); }
			catch(Exception e) { user.setEmail("NO_EMAIL"); }
			
			// Try to set gender if user allow gender access
			try { user.setGender(obj.getString("gender")); }
			catch(Exception e) { user.setGender("other"); }
			
			user.setName(obj.getString("name"));
			user.setFacebookId(obj.getString("id"));
			
			// Create an random password for user
			String randomPassword = UUID.randomUUID().toString();
			System.out.println("User Password: "+ randomPassword);
			user.setPassword(randomPassword);
			
			List<Role> roles = new ArrayList<>();
			roles.add(new Role()); // Add FACEBOOK_USER Role to user
			user.setRoles(roles);
			
			// Call userService to save user record to database
			// userService.signup(user);
			
			//Create login session (manual login)
			Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getRoles());
			SecurityContextHolder.getContext().setAuthentication(auth);
			System.out.println("User login successfully");
			
			return "redirect:/index";
		} catch (Exception e) {
			e.printStackTrace();
			return "/login";
		}
	}

}
