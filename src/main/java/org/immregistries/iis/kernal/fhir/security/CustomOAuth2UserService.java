package org.immregistries.iis.kernal.fhir.security;

import org.immregistries.iis.kernal.fhir.AppProperties;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;


@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest , OAuth2User> {
	Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		logger.info("{}",userRequest);
		logger.info("{}",userRequest.getAdditionalParameters());
		OrgAccess orgAccess = null;
		if (userRequest.getClientRegistration().getClientName().equals("GitHub")) {
			orgAccess = ServletHelper.authenticateOrgAccess(
				"github-" + (String) userRequest.getAdditionalParameters().get("login"),
				(String) userRequest.getAdditionalParameters().get("login"),
				(String) userRequest.getAdditionalParameters().get("id"),
				PopServlet.getDataSession());

			return orgAccess;
		}
		return null;
	}
}
