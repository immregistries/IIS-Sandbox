package org.immregistries.iis.kernal.logic.shlink.evc;

import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORItem;
import com.authlete.cose.*;
import com.authlete.cose.constants.COSEAlgorithms;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.immregistries.iis.kernal.model.persisted.IisKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.util.Collections;
import java.util.zip.DataFormatException;

@Service
public class EvcService {

	@Autowired
	NuvaService nuvaService;
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private final CBORMapper cborMapper = new CBORMapper();

	public EvcService() {
		Security.addProvider(new BouncyCastleProvider());
	}

	public byte[] cbor(EvCPayload evCPayload) throws DataFormatException, JsonProcessingException {
		return cborMapper.writeValueAsBytes(evCPayload);
	}

	public byte[] cbor(byte[] input) throws DataFormatException, IOException {
		// Convert the map to a CBOR-encoded byte array
		byte[] cborData = cborMapper.writeValueAsBytes(input);
		logger.info("CBOR byte array created successfully.\ninput: {}\ncbor: {}\nparsed: {}", new String(input), new String(cborData), cborMapper.readValue(cborData, String.class));
		return cborData;
	}

	public byte[] createCoseSign1(IisKey iisKey, byte[] cborPayload) throws IOException {
		ECPrivateKey ec = (ECPrivateKey) iisKey.keyPair().getPrivate();
		logger.info("ecKey {}", ec.getEncoded());
		COSESign1 sign1 = new COSESign1Builder()
			// Protected header
			.protectedHeader(
				// <<{1:-7}>>
				new COSEProtectedHeaderBuilder().alg(COSEAlgorithms.ES256).build()
			)
			// Unprotected header
			.unprotectedHeader(
				// {4:'11'}
				new COSEUnprotectedHeaderBuilder().kid("11").build()
			)
			// Payload
			.payload(cborPayload)
			// Signature
			.signature(
				iisKey.keyPair().getPrivate().getEncoded()
			)
			// Construct a COSESign1 instance.
			.build();

		byte[] encode = sign1.encode();
		COSEVerifier coseVerifier = new COSEVerifier(iisKey.keyPair().getPublic());
		CBORDecoder cborDecoder = new CBORDecoder(encode);
		CBORItem cborItem = cborDecoder.next();
		boolean verify = false;
		try {
			verify = coseVerifier.verify(sign1, "test".getBytes(StandardCharsets.UTF_8));
		} catch (COSEException e) {
			logger.error(e.getMessage());
		}
		logger.info("Code Sign encode: {}\n VERIFIED: {}\n {}\n {}\n", encode, verify, cborDecoder.all(), cborItem.prettify());
		return encode;
	}


	public byte[] createCoseSign1Old(IisKey iisKey, byte[] cborPayload) throws IOException, InvalidKeyException, SignatureException, NoSuchAlgorithmException, NoSuchProviderException {
		PrivateKey privateKey = iisKey.keyPair().getPrivate();

		// 1. Define the protected header as a CBOR Map
		// We'll use a simple CBOR map with the algorithm identifier (-7 for ES256)

		// This is a minimal protected header. In a real-world scenario, you might add more claims.
		byte[] protectedHeader = cborMapper.writeValueAsBytes(Collections.singletonMap(1, -7)); // alg: ES256

		// 2. Define the unprotected header (an empty CBOR map for this example)
		byte[] unprotectedHeader = cborMapper.writeValueAsBytes(Collections.emptyMap());

		// 3. Construct the 'Sig_structure' for signing, as defined in RFC 9052 Section 4.4
		ByteArrayOutputStream sigStructureStream = new ByteArrayOutputStream();

		// This is a simplified representation of the CBOR array for 'Sig_structure'
		// [
		//   "Signature1",
		//   protected_header_bstr,
		//   aad_bstr,
		//   payload_bstr
		// ]
		sigStructureStream.write(0x84); // CBOR array of 4 items
		sigStructureStream.write(0x6a); // CBOR text string of length 10
		sigStructureStream.write("Signature1".getBytes()); // TODO sigh with key ?
		sigStructureStream.write(0x40 + protectedHeader.length); // CBOR byte string
		sigStructureStream.write(protectedHeader);
		sigStructureStream.write(0x40); // CBOR empty byte string for AAD
		sigStructureStream.write(0x40 + cborPayload.length); // CBOR byte string
		sigStructureStream.write(cborPayload);

		byte[] toBeSigned = sigStructureStream.toByteArray();

		// 4. Sign the 'ToBeSigned' data
		java.security.Signature signature = java.security.Signature.getInstance("SHA256withECDSA", "BC");
		signature.initSign(privateKey);
		signature.update(toBeSigned);
		byte[] coseSignature = signature.sign();

		// 5. Assemble the final COSE_Sign1 message
		ByteArrayOutputStream coseStream = new ByteArrayOutputStream();

		// The final structure is a CBOR array of 4 elements
		// [
		//   protected_header_bstr,
		//   unprotected_header_map,
		//   payload_bstr,
		//   signature_bstr
		// ]

		// The headers and payload are all CBOR-tagged as byte strings.
		// We need to construct the final array manually for this simple example.
		coseStream.write(0x84); // CBOR array of 4 items

		coseStream.write(0x40 + protectedHeader.length); // CBOR byte string
		coseStream.write(protectedHeader);

		coseStream.write(0xa0); // CBOR empty map (unprotected header)

		coseStream.write(0x40 + cborPayload.length); // CBOR byte string
		coseStream.write(cborPayload);

		coseStream.write(0x40 + coseSignature.length); // CBOR byte string
		coseStream.write(coseSignature);

		return coseStream.toByteArray();
	}

}
