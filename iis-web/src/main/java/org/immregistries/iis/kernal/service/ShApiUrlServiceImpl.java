package org.immregistries.iis.kernal.service;

import org.immregistries.iis.kernal.IisConfigService;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.logic.shlink.IShApiUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import static org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkManifestRestController.SHLINKS_CONTROLLER_REST_BASE_URL;

@Service
public class ShApiUrlServiceImpl implements IShApiUrlService {
	@Autowired
	private IisConfigService iisConfigService;

	public void replaceUrlWithShCardPattern(UriComponentsBuilder uriBuilder) {
		uriBuilder.replacePath(iisConfigService.getCONTEXT_PATH() + SHLINKS_CONTROLLER_REST_BASE_URL + IisPathVariable.PlaceHolder.MANIFEST_ID_PLACEHOLDER);
	}


	public void replaceUrlWithShLinkPattern(UriComponentsBuilder uriBuilder) {
		uriBuilder.replacePath(iisConfigService.getCONTEXT_PATH() + IisRestPath.SHLINK_CONTENT_PATH + IisPathVariable.PlaceHolder.CONTENT_ID_PLACEHOLDER);
	}

}
