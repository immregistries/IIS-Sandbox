package org.immregistries.iis.kernal.logic.shlink;


import org.springframework.web.util.UriComponentsBuilder;

public interface IShApiUrlService {

	void replaceUrlWithShCardPattern(UriComponentsBuilder uriBuilder);
	void replaceUrlWithShLinkPattern(UriComponentsBuilder uriBuilder);


}
