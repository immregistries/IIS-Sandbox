package org.immregistries.iis.kernal.service;

import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.shlink.IShApiUrlService;
import org.immregistries.iis.kernal.services.api.IDeployedApiUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import static org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkManifestRestController.SHLINKS_CONTROLLER_REST_BASE_URL;

@Service
public class ShApiUrlServiceImpl implements IShApiUrlService {
	@Autowired
	private IDeployedApiUrlService deployedUrlService;

	public void replaceUrlWithShCardPattern(UriComponentsBuilder uriBuilder) {
		uriBuilder.replacePath(deployedUrlService.getContextPath() + SHLINKS_CONTROLLER_REST_BASE_URL + IisPathVariable.PlaceHolder.MANIFEST_ID_PLACEHOLDER);
	}


	public void replaceUrlWithShLinkPattern(UriComponentsBuilder uriBuilder) {
		uriBuilder.replacePath(deployedUrlService.getContextPath() + IisRestPath.SH_LINK_CONTENT_PATH + IisPathVariable.PlaceHolder.CONTENT_ID_PLACEHOLDER);
	}

}
