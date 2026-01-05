package org.immregistries.iis.kernal.logic;

import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.CodeMapBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class CodeMapManagerService implements InitializingBean {
	private static CodeMapManagerService instance;

	public static CodeMapManagerService get() {
		return instance;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		instance = this;
	}

	private final CodeMapBuilder builder = CodeMapBuilder.INSTANCE;
	private final CodeMap codeMap;

	public CodeMap getCodeMap() {
		return codeMap;
	}


	public CodeMapManagerService() {
		InputStream is = this.getClass().getResourceAsStream("/Compiled.xml");
		if (is == null) {
			System.err.println("Unable to find Compiled.xml!");
		}
		codeMap = builder.getCodeMap(is);
	}

}
