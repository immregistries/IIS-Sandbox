package org.immregistries.iis.kernal.logic.shlink.evc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.zip.DataFormatException;

@Service
public class EvcService {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private CBORMapper cborMapper = new CBORMapper();

	public byte[] cbor(EvCPayload evCPayload) throws DataFormatException, JsonProcessingException {
		return cborMapper.writeValueAsBytes(evCPayload);
	}

	public byte[] cbor(byte[] input) throws DataFormatException, JsonProcessingException {
		// Convert the map to a CBOR-encoded byte array
		byte[] cborData = cborMapper.writeValueAsBytes(input);
		logger.info("CBOR byte array created successfully. {}", new String(cborData));
		return cborData;
	}

}
