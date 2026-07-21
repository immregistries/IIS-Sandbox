package org.immregistries.iis.kernal.controllers.rest;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.util.Terser;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static org.immregistries.iis.kernal.controllers.IisRestPath.BasePath.VCI_DEMO_PATH;

@RestController
@RequestMapping(IisRestPath.BasePath.REST_PATH + VCI_DEMO_PATH)
public class VciDemoRestController {

	private static final Map<String, String> GENDER_MAP;

	static {
		Map<String, String> map = new HashMap<>();
		map.put("F", "female");
		map.put("M", "male");
		map.put("U", "unknown");
		GENDER_MAP = Collections.unmodifiableMap(map);
	}

	private static final String EXAMPLE_RSP =
		"MSH|^~\\&||IIS Sandbox v0.4.1|||20210327104950-0600||RSP^K11^RSP_K11|16168637904173|P|2.5.1|||NE|NE|||||Z32^CDCPHINVS\r\n"
			+ "MSA|AA|1616863788100.1\r\n"
			+ "QAK|1616863788100.1|OK|Z34^Request a Complete Immunization History^CDCPHINVS\r\n"
			+ "QPD|Z34^Request Immunization History^CDCPHINVS|1616863788100.1|O26S1^^^AIRA-TEST^MR|WestmorelandAIRA^AbigaleAIRA^Hemangi^^^^L|GalvestonAIRA^AlumitAIRA^^^^^M|20170317|F|1049 Daterland Ave^^Coloma^MI^49039^USA^P|^PRN^PH^^^269^5713805|||||\r\n"
			+ "PID|1||CVM7PU8NTVR6^^^IIS^SR~O26S1^^^AIRA-TEST^MR||WestmorelandAIRA^AbigaleAIRA^Hemangi^^^^L|GalvestonAIRA^^^^^^M|20170317|F|||1049 Daterland Ave^^Coloma^MI^49039^USA^P||^PRN^PH^^^269^5713805||||||||||||\r\n"
			+ "NK1|1|HoggAIRA^AlumitAIRA^^^^^L|MTH^Mother^HL70063\r\n"
			+ "ORC|RE|11533^IIS|O26S1.3^AART Primary\r\n"
			+ "RXA|0|1|20210327||03^MMR^CVX|0.5|mL^milliliters^UCUM||00^New immunization record^NIP001||||||U1747GW||MSD^Merck and Co., Inc.^MVX|||CP|A\r\n"
			+ "RXR|C38299^Subcutaneous^NCIT|LA^Left Upper Arm^HL70163\r\n"
			+ "OBX|1|CE|30956-7^Vaccine type^LN|1|06^06^CVX||||||F\r\n"
			+ "ORC|RE||9999^IIS\r\n";

	public static class VciConversionResult {
		private Map<String, Object> fhirPatient;
		private Map<String, Object> fhirImmunization;
		private Map<String, Object> verifiableCredential;

		public VciConversionResult() {
		}

		public VciConversionResult(Map<String, Object> fhirPatient, Map<String, Object> fhirImmunization,
		                           Map<String, Object> verifiableCredential) {
			this.fhirPatient = fhirPatient;
			this.fhirImmunization = fhirImmunization;
			this.verifiableCredential = verifiableCredential;
		}

		public Map<String, Object> getFhirPatient() {
			return fhirPatient;
		}

		public Map<String, Object> getFhirImmunization() {
			return fhirImmunization;
		}

		public Map<String, Object> getVerifiableCredential() {
			return verifiableCredential;
		}
	}

	@GetMapping(value = "/sample", produces = "text/plain")
	public String getSample() {
		return EXAMPLE_RSP;
	}

	@PostMapping(value = "/convert", consumes = "text/plain")
	public VciConversionResult convert(@RequestBody String rspMessage) throws Exception {
		HapiContext context = new DefaultHapiContext();
		Parser p = context.getGenericParser();
		Message hapiMsg = p.parse(rspMessage);
		Terser terser = new Terser(hapiMsg);

		String bd = terser.get("/.PID-7");
		String birthDate = bd.substring(0, 4) + "-" + bd.substring(4, 6) + "-" + bd.substring(6, 8);
		String sex = terser.get("/.PID-8");
		String gender = GENDER_MAP.getOrDefault(sex, "other");
		String familyName = terser.get("/.PID-5-1");
		String firstName = terser.get("/.PID-5-2");
		String middleName = terser.get("/.PID-5-3");
		String cvxCode = terser.get("/.RXA-5-1");
		String im = terser.get("/.RXA-3");
		String immDate = im.substring(0, 4) + "-" + im.substring(4, 6) + "-" + im.substring(6, 8);
		String lotNumber = terser.get("/.RXA-15");

		Map<String, Object> fhirPatient = new LinkedHashMap<>();
		fhirPatient.put("resourceType", "Patient");
		fhirPatient.put("gender", gender);
		fhirPatient.put("birthDate", birthDate);
		fhirPatient.put("name", List.of(Map.of(
			"family", familyName,
			"given", Arrays.asList(firstName, middleName)
		)));

		Map<String, Object> fhirImm = new LinkedHashMap<>();
		fhirImm.put("resourceType", "Immunization");
		fhirImm.put("status", "completed");
		fhirImm.put("vaccineCode", Map.of(
			"coding", List.of(Map.of(
				"system", "http://hl7.org/fhir/sid/cvx",
				"code", cvxCode
			))
		));
		fhirImm.put("patient", Map.of("reference", "resource:0"));
		fhirImm.put("occurrenceDateTime", immDate);
		fhirImm.put("lotNumber", lotNumber);

		Map<String, Object> vc = new LinkedHashMap<>();
		vc.put("iss", "https://sabbia.westus2.cloudapp.azure.com/issuer");
		vc.put("nbf", System.currentTimeMillis() / 1000L);
		vc.put("vc", Map.of(
			"@context", List.of("https://www.w3.org/2018/credentials/v1"),
			"type", List.of("VerifiableCredential", "https://smarthealth.cards#health-card"),
			"credentialSubject", Map.of(
				"fhirVersion", "4.0.1",
				"fhirBundle", Map.of(
					"resourceType", "Bundle",
					"type", "collection",
					"entry", List.of(
						Map.of("fullUrl", "resource:0", "resource", fhirPatient),
						Map.of("fullUrl", "resource:1", "resource", fhirImm)
					)
				)
			)
		));

		return new VciConversionResult(fhirPatient, fhirImm, vc);
	}
}
