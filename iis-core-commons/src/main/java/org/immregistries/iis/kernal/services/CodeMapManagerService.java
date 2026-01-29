package org.immregistries.iis.kernal.services;

import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.CodeMapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
/**
 * Grants access to CodeMaps extracted from XML file, usually Compiled.xml
 */
public class CodeMapManagerService implements InitializingBean {
	public static final String COMPILED_XML_PATH = "/Compiled.xml";
	private Logger logger = LoggerFactory.getLogger(this.getClass());

//	private static CodeMapManagerService instance;
//	public static CodeMapManagerService get() {
//		return instance;
//	}
	@Override
	public void afterPropertiesSet() {
//		instance = this;
	}

	private final CodeMapBuilder builder = CodeMapBuilder.INSTANCE;
	private final CodeMap codeMap;

	public CodeMap getCodeMap() {
		return codeMap;
	}

	public CodeMapManagerService() {
		InputStream is = this.getClass().getResourceAsStream(COMPILED_XML_PATH);
		if (is == null) {
			logger.error("Could not load compiled CodeMap Unable to find file from {}", COMPILED_XML_PATH);
		}
		codeMap = builder.getCodeMap(is);
	}

}
