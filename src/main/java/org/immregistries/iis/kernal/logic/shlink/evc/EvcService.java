package org.immregistries.iis.kernal.logic.shlink.evc;

import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORItem;
import com.authlete.cose.*;
import com.authlete.cose.constants.COSEAlgorithms;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import nl.minvws.encoding.Base45;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.immregistries.iis.kernal.logic.shlink.CompressionUtil;
import org.immregistries.iis.kernal.model.persisted.IisKey;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.DataFormatException;

@Service
public class EvcService {

	public static final String VC_1 = "VC1:";


	@Autowired
	NuvaService nuvaService;

	Logger logger = LoggerFactory.getLogger(this.getClass());
	private final CBORMapper cborMapper = new CBORMapper();

	public EvcService() {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Uses Jackson specification to cborize evCPayload
	 *
	 * @param evCPayload
	 * @return
	 * @throws DataFormatException
	 * @throws JsonProcessingException
	 */
	public byte[] toCbor(EvCPayload evCPayload) throws IOException {
		byte[] cbor = cborMapper.writeValueAsBytes(evCPayload);
		logger.info("CBOR byte array created successfully.\ninputObject: {}\ncbor: {}\nparsed: {}", new ObjectMapper().writeValueAsString(evCPayload), new String(cbor), cborMapper.createParser(cbor).readValueAsTree());
		return cbor;
	}

	public EvCPayload undoCbor(byte[] cbor) throws IOException {
		logger.info("parse CBOR cbor: {}\nparsed: {}", new String(cbor), cborMapper.createParser(cbor).readValueAsTree());
		EvCPayload evCPayload = cborMapper.readValue(cbor, EvCPayload.class);
		return evCPayload;
	}

	public byte[] decodeQrCode(byte[] qrcode) {
		String s = new String(qrcode);
		String s2 = StringUtils.substringAfter(s,VC_1);
		return Base45.getDecoder().decode(s2);
	}

	public String encodeQrCode(EvCPayload evCPayload, IisKey iisSigningKey) throws IOException, COSEException {
		byte[] cborPayload = toCbor(evCPayload);
		logger.info("HERE: {} {} {}", new String(cborPayload), cborPayload.length, undoCbor(cborPayload));
		byte[] cosePayload = createCoseSign1(cborPayload, iisSigningKey);
		byte[] deflated = CompressionUtil.deflate(cosePayload);
		return VC_1 + Base45.getEncoder().encodeToString(deflated);
	}


	public EvCPayload decodeFullQrCode(byte[] qrcode, IisKey iisKey) throws COSEException, IOException, DataFormatException {
		byte[] compressed = decodeQrCode(qrcode);
		byte[] cose = CompressionUtil.inflate(compressed);
		byte[] decoded = decodeCoseSign1(cose, iisKey.publicKey());
		logger.info("DECODEFULL: {}  {}", new String(decoded), decoded.length);

		EvCPayload evCPayload = undoCbor(decoded);
		return evCPayload;
	}

	public byte[] createCoseSign1(byte[] cborPayload, IisKey iisKey) throws IOException, COSEException {
		ECPrivateKey priKey = (ECPrivateKey) iisKey.keyPair().getPrivate();

		// Create a signer with the private key.
		COSESigner signer = new COSESigner(priKey);

		// Signature algorithm
		int algorithm = COSEAlgorithms.ES256;

		// Protected header
		COSEProtectedHeader protectedHeader =
			new COSEProtectedHeaderBuilder().alg(algorithm).build();

		// Unprotected header
		COSEUnprotectedHeader unprotectedHeader =
			new COSEUnprotectedHeaderBuilder().kid("11").build();

		// Sig_structure
		SigStructure structure = new SigStructureBuilder()
			.signature1()
			.bodyAttributes(protectedHeader)
			.payload(cborPayload).build();
		// Sign the Sig_structure (= generate a signature).
		byte[] signature = signer.sign(structure, COSEAlgorithms.ES256);
		COSESign1 sign1 = new COSESign1Builder()
			.protectedHeader(protectedHeader)
			.unprotectedHeader(unprotectedHeader)
			.payload(cborPayload)
			.signature(signature)
			.build();

		byte[] encode = sign1.encode();
//		logger.info("ENCODE TEST {}", new String(encode));
		decodeCoseSign1(encode, iisKey.publicKey());
		return encode;
	}

	private byte[] decodeCoseSign1(byte[] encode, PublicKey publicKey) throws IOException, COSEException {
		/*
		 * Decode
		 */
		CBORDecoder cborDecoder = new CBORDecoder(encode);
		CBORItem item = cborDecoder.next();
//		logger.info("DECODED \nCborItem: {}\n encode: {}\n, parse {}\n, all size {}\n", cborMapper.createParser(item.encode()).readValueAsTree(), new String(item.encode()), item.parse(), cborDecoder.all().size());
		COSESign1 coseSign1 = COSESign1.build(item);
//		while (item != null) {
//			item = cborDecoder.next();
//		}
		/*
		 * Verify signature
		 */
		if (publicKey != null){
			COSEVerifier coseVerifier = new COSEVerifier(publicKey);
			boolean verify = false;
			try {
				verify = coseVerifier.verify(coseSign1, null);
//				logger.info("VERIFIER Cose Sign encode: {}\n VERIFIED: {}\n  payload: {}\n", new String(encode), verify, coseSign1.getPayload());
			} catch (COSEException e) {
				logger.error(e.getMessage());
			}
		}

//		CBORParser cborParser = new CBORParser(encode);
//		Object object = cborParser.next();
//		while (object != null) {
//			logger.info("PARSED \nOBJECT: {}\n", object);
//			object = cborParser.next();
//		}
		byte[] bytes = coseSign1.getPayload().encode();
		/*
		 * Removing bytes added through Cose to only get the payload
		 */
		return Arrays.copyOfRange(bytes,2, bytes.length);
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
