package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.mqe.hl7util.parser.HL7Reader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.immregistries.iis.kernal.controllers.IisRestPath.BasePath.LAB_CONVERTER_PATH;

@RestController
@RequestMapping({
	IisRestPath.REST_TENANT_PATH + LAB_CONVERTER_PATH,
	IisRestPath.BasePath.REST_PATH + LAB_CONVERTER_PATH
})
public class LabConverterRestController {

	public static class LabConversionResult {
		private String vxuMessage;
		private int testCount;

		public LabConversionResult() {
		}

		public LabConversionResult(String vxuMessage, int testCount) {
			this.vxuMessage = vxuMessage;
			this.testCount = testCount;
		}

		public String getVxuMessage() {
			return vxuMessage;
		}

		public int getTestCount() {
			return testCount;
		}
	}

	private static final Map<String, String> COVID_19_TESTS = new HashMap<>();

	static {
		COVID_19_TESTS.put("94763-0", "SARS coronavirus 2");
		COVID_19_TESTS.put("94661-6", "SARS coronavirus 2 Ab");
		COVID_19_TESTS.put("94762-2", "SARS coronavirus 2 Ab");
		COVID_19_TESTS.put("94769-7", "SARS coronavirus 2 Ab");
		COVID_19_TESTS.put("94504-8", "SARS coronavirus 2 Ab panel");
		COVID_19_TESTS.put("94558-4", "SARS coronavirus 2 Ag");
		COVID_19_TESTS.put("94509-7", "SARS coronavirus 2 E gene");
		COVID_19_TESTS.put("94758-0", "SARS coronavirus 2 E gene");
		COVID_19_TESTS.put("94765-5", "SARS coronavirus 2 E gene");
		COVID_19_TESTS.put("94315-9", "SARS coronavirus 2 E gene");
		COVID_19_TESTS.put("94562-6", "SARS coronavirus 2 Ab.IgA");
		COVID_19_TESTS.put("94768-9", "SARS coronavirus 2 Ab.IgA");
		COVID_19_TESTS.put("94720-0", "SARS coronavirus 2 Ab.IgA");
		COVID_19_TESTS.put("95125-1", "SARS coronavirus 2 Ab.IgA+IgM");
		COVID_19_TESTS.put("94761-4", "SARS coronavirus 2 Ab.IgG");
		COVID_19_TESTS.put("94563-4", "SARS coronavirus 2 Ab.IgG");
		COVID_19_TESTS.put("94507-1", "SARS coronavirus 2 Ab.IgG");
		COVID_19_TESTS.put("94505-5", "SARS coronavirus 2 Ab.IgG");
		COVID_19_TESTS.put("94503-0", "SARS coronavirus 2 Ab.IgG & IgM panel");
		COVID_19_TESTS.put("94547-7", "SARS coronavirus 2 Ab.IgG+IgM");
		COVID_19_TESTS.put("94564-2", "SARS coronavirus 2 Ab.IgM");
		COVID_19_TESTS.put("94508-9", "SARS coronavirus 2 Ab.IgM");
		COVID_19_TESTS.put("94506-3", "SARS coronavirus 2 Ab.IgM");
		COVID_19_TESTS.put("94510-5", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94311-8", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94312-6", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94760-6", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94533-7", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94756-4", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94757-2", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94766-3", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94316-7", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94307-6", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94308-4", "SARS coronavirus 2 N gene");
		COVID_19_TESTS.put("94644-2", "SARS coronavirus 2 ORF1ab region");
		COVID_19_TESTS.put("94511-3", "SARS coronavirus 2 ORF1ab region");
		COVID_19_TESTS.put("94559-2", "SARS coronavirus 2 ORF1ab region");
		COVID_19_TESTS.put("94639-2", "SARS coronavirus 2 ORF1ab region");
		COVID_19_TESTS.put("94646-7", "SARS coronavirus 2 RdRp gene");
		COVID_19_TESTS.put("94645-9", "SARS coronavirus 2 RdRp gene");
		COVID_19_TESTS.put("94534-5", "SARS coronavirus 2 RdRp gene");
		COVID_19_TESTS.put("94314-2", "SARS coronavirus 2 RdRp gene");
		COVID_19_TESTS.put("94745-7", "SARS coronavirus 2 RNA");
		COVID_19_TESTS.put("94746-5", "SARS coronavirus 2 RNA");
		COVID_19_TESTS.put("94819-0", "SARS coronavirus 2 RNA");
		COVID_19_TESTS.put("94565-9", "SARS coronavirus 2 RNA");
		COVID_19_TESTS.put("94759-8", "SARS coronavirus 2 RNA");
		COVID_19_TESTS.put("94500-6", "SARS coronavirus 2 RNA");
		COVID_19_TESTS.put("94845-5", "SARS coronavirus 2 RNA");
		COVID_19_TESTS.put("94822-4", "SARS coronavirus 2 RNA");
		COVID_19_TESTS.put("94660-8", "SARS coronavirus 2 RNA");
		COVID_19_TESTS.put("94309-2", "SARS coronavirus 2 RNA");
		COVID_19_TESTS.put("94531-1", "SARS coronavirus 2 RNA panel");
		COVID_19_TESTS.put("94306-8", "SARS coronavirus 2 RNA panel");
		COVID_19_TESTS.put("94642-6", "SARS coronavirus 2 S gene");
		COVID_19_TESTS.put("94643-4", "SARS coronavirus 2 S gene");
		COVID_19_TESTS.put("94640-0", "SARS coronavirus 2 S gene");
		COVID_19_TESTS.put("94767-1", "SARS coronavirus 2 S gene");
		COVID_19_TESTS.put("94641-8", "SARS coronavirus 2 S gene");
		COVID_19_TESTS.put("94764-8", "SARS coronavirus 2 whole genome");
		COVID_19_TESTS.put("95209-3", "SARS coronavirus+SARS coronavirus 2 Ag");
		COVID_19_TESTS.put("94313-4", "SARS-like coronavirus N gene");
		COVID_19_TESTS.put("94310-0", "SARS-like coronavirus N gene");
		COVID_19_TESTS.put("94502-2", "SARS-related coronavirus RNA");
		COVID_19_TESTS.put("94647-5", "SARS-related coronavirus RNA");
		COVID_19_TESTS.put("94532-9", "SARS-related coronavirus+MERS coronavirus RNA");
	}

	private static final String EXAMPLE_LAB_MESSAGE =
		"MSH|^~\\&|STARLIMS.AR.STAG^2.16.840.1.114222.4.3.3.2.5.2^ISO|AR.LittleRock.SPHL^2.16.840.1.114222.4.1.20083^ISO|US WHO Collab LabSys^2.16.840.1.114222.4.3.3.7^ISO|CDC-EPI Surv Branch^2.16.840.1.114222.4.1.10416^ISO|20190422132236-0500||ORU^R01^ORU_R01|1312-2|T|2.5.1|||NE|NE|USA||||PHLabReport-NoAck^ELR251R1_Rcvr_Prof^2.16.840.1.113883.9.11^ISO~PHLIP_ELSM_251^PHLIP_Profile_Flu^2.16.840.1.113883.9.179^ISO\r"
			+ "SFT|Software Vendor|v12|Software Name|Binary ID unknown||20181008\r"
			+ "PID|1||PID13295037^^^STARLIMS.AR.STAG&2.16.840.1.114222.4.3.3.2.5.2&ISO^PI||~^^^^^^S||19340726|F||2106-3^White^CDCREC^^^^^^White|^^^AR^72016^USA|||||||||||U^Unknown^HL70189^^^^^^Unknown\r"
			+ "ORC|RE|1905700000256-13^PHLIP-Test-EHR^2.16.840.1.113883.3.72.5.24^ISO|1905700000256-177^STARLIMS.AR.STAG^2.16.840.1.114222.4.3.3.2.5.2^ISO|||||||||1412941681^Smith^John^C^^DR^^^NPI&2.16.840.1.113883.4.6&ISO^L^^^NPI^^^^^^^^MD||^WPN^PH^^1^707^2643378|||||||Little Rock General Hospital Lab^D^^^^NPI&2.16.840.1.113883.4.6&ISO^NPI^^^1255402921|2217 Trancas^Suite 22^Little Rock^AR^72205^USA^M|^WPN^PH^^1^707^5549876\r"
			+ "OBR|1|1905700000256-13^PHLIP-Test-EHR^2.16.840.1.113883.3.72.5.24^ISO|1905700000256-177^STARLIMS.AR.STAG^2.16.840.1.114222.4.3.3.2.5.2^ISO|94309-2^SARS-CoV-2 RNA XXX NAA+probe-Imp^LN|||201902281257-0500|||||||||1412941681^Smith^John^C^^DR^^^NPI&2.16.840.1.113883.4.6&ISO^L^^^NPI^^^^^^^^MD|^WPN^PH^^1^707^2643378|||||20190402082143-0500|||F\r"
			+ "OBX|1|CWE|94309-2^SARS-CoV-2 RNA XXX NAA+probe-Imp^LN||260373001^Detected^SCT||||||F|||201902281257-0500|||||201904020721-0500||||Public Health Laboratory^D^^^^CLIA&2.16.840.1.113883.19.4.6&ISO^XX^^^05D0897628|3434 Industrial Loop^^Little Rock^AR^72205^USA^B\r"
			+ "SPM|1|^1905700000256-12&STARLIMS.AR.STAG&2.16.840.1.114222.4.3.3.2.5.2&ISO||258500001^Nasopharyngeal swab (specimen)^SCT|||||||||||||201902281257-0500|201903011118-0500";

	@GetMapping(value = "/sample", produces = "text/plain")
	public String getSample() {
		return EXAMPLE_LAB_MESSAGE;
	}

	@PostMapping(consumes = "text/plain")
	public LabConversionResult convert(@RequestBody String oruMessage) {
		HL7Reader reader = new HL7Reader(oruMessage);

		if (!reader.advanceToSegment("MSH")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Does not appear to be HL7 message, MSH Segment not found");
		}
		if (!reader.getValue(9).equals("ORU") || !reader.getValue(9, 2).equals("R01")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Unable to convert, not an ORU^R01 message");
		}

		String dateTimeOfMessage = reader.getOriginalField(7);
		StringBuilder sb = new StringBuilder();
		sb.append("MSH|^~\\&|");
		sb.append(reader.getOriginalField(3)).append("|");
		sb.append(reader.getOriginalField(4)).append("|");
		sb.append(reader.getOriginalField(5)).append("|");
		sb.append(reader.getOriginalField(6)).append("|");
		sb.append(dateTimeOfMessage).append("|");
		sb.append("|");
		sb.append("VXU^V04^VXU_V04|");
		sb.append(reader.getOriginalField(10)).append("|");
		sb.append(reader.getOriginalField(11)).append("|");
		sb.append("2.5.1|");
		sb.append("|||ER|AL||||");
		sb.append("Z22^CDCPHINVS\r");

		if (dateTimeOfMessage.length() > 8) {
			dateTimeOfMessage = dateTimeOfMessage.substring(0, 8);
		}

		if (reader.advanceToSegment("PID")) {
			sb.append(reader.getOriginalSegment()).append("\r");
		}

		int count = 0;
		while (reader.advanceToSegment("OBX")) {
			String question = reader.getValue(3);
			if (COVID_19_TESTS.containsKey(question)) {
				count++;
				sb.append("ORC|RE||9999^IIS\r");
				sb.append("RXA|0|1|").append(dateTimeOfMessage)
					.append("||998^No Vaccination Administered^CVX|999||||||||||||||NA\r");
				sb.append(reader.getOriginalSegment()).append("\r");
			}
		}

		return new LabConversionResult(sb.toString(), count);
	}
}
