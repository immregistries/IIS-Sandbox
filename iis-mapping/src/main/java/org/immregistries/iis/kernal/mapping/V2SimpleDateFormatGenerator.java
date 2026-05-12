package org.immregistries.iis.kernal.mapping;

import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Service
public class V2SimpleDateFormatGenerator {

	public SimpleDateFormat generateSimpleDateFormat() {
		return new SimpleDateFormat("yyyyMMdd");
	}

}
