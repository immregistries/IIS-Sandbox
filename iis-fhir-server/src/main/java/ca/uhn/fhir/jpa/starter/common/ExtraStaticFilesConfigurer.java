package ca.uhn.fhir.jpa.starter.common;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//@Configuration
//@ConditionalOnProperty(prefix = "hapi.fhir", name = "staticLocation")
public class ExtraStaticFilesConfigurer implements WebMvcConfigurer {

//	@Autowired
//	AppProperties appProperties;
//
//	@Override
//	public void addResourceHandlers(ResourceHandlerRegistry theRegistry) {
//		theRegistry
//			.addResourceHandler("/static/**")
//			.addResourceLocations(appProperties.getStaticLocation());
//	}
}