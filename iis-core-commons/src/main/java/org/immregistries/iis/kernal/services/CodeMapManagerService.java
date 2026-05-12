package org.immregistries.iis.kernal.services;

import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.CodeMapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
/**
 * Grants access to CodeMaps extracted from XML file, usually Compiled.xml
 */
public class CodeMapManagerService {
	public static final String COMPILED_XML_PATH = "/Compiled.xml";
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private final CodeMap codeMap;

	public CodeMap getCodeMap() {
		return codeMap;
	}

	public CodeMapManagerService(ResourceLoader resourceLoader) {
		CodeMapBuilder builder = CodeMapBuilder.INSTANCE;
		InputStream is = resourceLoader.getClassLoader().getResourceAsStream(COMPILED_XML_PATH);
		if (is == null) {
			logger.error("Could not load compiled CodeMap Unable to find file from {}", COMPILED_XML_PATH);
		}
		codeMap = builder.getCodeMap(is);
	}

}
