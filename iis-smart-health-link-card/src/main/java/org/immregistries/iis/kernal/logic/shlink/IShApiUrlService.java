package org.immregistries.iis.kernal.logic.shlink;


import org.springframework.web.util.UriComponentsBuilder;

/**
 * Interface to convert Uri's with API Context path and base URL
 */
public interface IShApiUrlService {

	void replaceUrlWithShCardPattern(UriComponentsBuilder uriBuilder);
	void replaceUrlWithShLinkPattern(UriComponentsBuilder uriBuilder);

}
