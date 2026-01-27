package org.immregistries.iis.kernal.logic.shlink;

import org.immregistries.iis.kernal.Application;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.springframework.web.util.UriComponentsBuilder;

import static org.immregistries.iis.kernal.controllers.IisPathVariable.Key.CONTENT_ID;
import static org.immregistries.iis.kernal.controllers.IisPathVariable.Key.MANIFEST_ID;
import static org.immregistries.iis.kernal.controllers.rest.shlink.ShLinkManifestRestController.SHLINKS_CONTROLLER_REST_BASE_URL;


public class ShApiUrlServiceImpl implements IShApiUrlService {
	public void replaceUrlWithShCardPattern(UriComponentsBuilder uriBuilder) {
		uriBuilder.replacePath(Application.IIS_PATH_BASE + SHLINKS_CONTROLLER_REST_BASE_URL + "/{" + MANIFEST_ID + "}");
	}


	public void replaceUrlWithShLinkPattern(UriComponentsBuilder uriBuilder) {
		uriBuilder.replacePath( Application.IIS_PATH_BASE  + IisRestPath.SHLINK_CONTENT_PATH + "/{" + CONTENT_ID + "}");
	}



}
