package org.immregistries.iis.fhir.common.web;

import org.immregistries.iis.fhir.common.AppProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileUrlResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.MalformedURLException;

@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir", name = "custom_content_path")
public class IisContentFilesConfigurer implements WebMvcConfigurer {

	public static final String CUSTOM_CONTENT = "/content";
	private String customContentPath;

	public IisContentFilesConfigurer(AppProperties appProperties) {
		customContentPath = appProperties.getCustom_content_path();
		if (customContentPath.endsWith("/"))
			customContentPath = customContentPath.substring(0, customContentPath.lastIndexOf('/'));
	}

	@Override
	public void addResourceHandlers(@NotNull ResourceHandlerRegistry theRegistry) {
		if (!theRegistry.hasMappingForPattern(CUSTOM_CONTENT + "/**")) {

			try {
				theRegistry
					.addResourceHandler(CUSTOM_CONTENT + "/**")
					.addResourceLocations(new FileUrlResource(customContentPath));
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
