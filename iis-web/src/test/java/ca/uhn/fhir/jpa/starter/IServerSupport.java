package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface  IServerSupport {

	default IAnyResource loadResource(String theLocation, FhirContext theFhirContext, DaoRegistry theDaoRegistry) throws IOException {
    String json = stringFromResource(theLocation);
		IAnyResource resource = (IAnyResource) theFhirContext.newJsonParser().parseResource(json);
		IFhirResourceDao<IAnyResource> dao = theDaoRegistry.getResourceDao(resource.getIdElement().getResourceType());
    if (dao == null) {
      return null;
    } else {
      dao.update(resource);
      return resource;
    }
  }

  default String stringFromResource(String theLocation) throws IOException {
    InputStream is = null;
    if (theLocation.startsWith(File.separator)) {
      is = new FileInputStream(theLocation);
    } else {
      DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
      Resource resource = resourceLoader.getResource(theLocation);
      is = resource.getInputStream();
    }
    return IOUtils.toString(is, Charsets.UTF_8);
  }
}
