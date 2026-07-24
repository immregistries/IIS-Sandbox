package org.immregistries.iis.kernal.controllers.rest.shlink;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.ips.generator.IIpsGeneratorSvc;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.hl7.fhir.r4.model.Bundle;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.security.RequestTenantUtil;
import org.immregitries.clvr.*;
import org.immregitries.clvr.mapping.FhirConversionUtil;
import org.immregitries.clvr.model.CLVRPayload;
import org.immregitries.clvr.model.CLVRToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(IisRestPath.REST_PATIENT_PATH + IisRestPath.BasePath.CLVR_PATH + "/test")
public class CLVRTestRestController {

	public static final String IPS_SAMPLE_R4_IIS = "{\n" +
		"  \"resourceType\": \"Bundle\",\n" +
		"  \"meta\": {\n" +
		"    \"profile\": [ \"http://hl7.org/fhir/uv/ips/StructureDefinition/Bundle-uv-ips\" ]\n" +
		"  },\n" +
		"  \"identifier\": {\n" +
		"    \"system\": \"urn:ietf:rfc:4122\",\n" +
		"    \"value\": \"3a513a49-64bc-4dce-92ea-7eacd0330ead\"\n" +
		"  },\n" +
		"  \"type\": \"document\",\n" +
		"  \"timestamp\": \"2025-12-04T14:26:10.844-05:00\",\n" +
		"  \"entry\": [ {\n" +
		"    \"fullUrl\": \"urn:uuid:ef3d0efb-7290-4576-b353-1f0de7c668e8\",\n" +
		"    \"resource\": {\n" +
		"      \"resourceType\": \"Composition\",\n" +
		"      \"text\": {\n" +
		"        \"status\": \"generated\",\n" +
		"        \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><h1>International Patient Summary Document</h1></div>\"\n" +
		"      },\n" +
		"      \"status\": \"final\",\n" +
		"      \"type\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"http://loinc.org\",\n" +
		"          \"code\": \"60591-5\",\n" +
		"          \"display\": \"Patient Summary Document\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"subject\": {\n" +
		"        \"reference\": \"Patient/1653\"\n" +
		"      },\n" +
		"      \"date\": \"2025-12-04T14:26:10.730-05:00\",\n" +
		"      \"title\": \"Patient Summary as of 12/04/2025\",\n" +
		"      \"confidentiality\": \"N\",\n" +
		"      \"section\": [ {\n" +
		"        \"title\": \"Allergies and Intolerances\",\n" +
		"        \"code\": {\n" +
		"          \"coding\": [ {\n" +
		"            \"system\": \"http://loinc.org\",\n" +
		"            \"code\": \"48765-2\",\n" +
		"            \"display\": \"Allergies and adverse reactions Document\"\n" +
		"          } ]\n" +
		"        },\n" +
		"        \"text\": {\n" +
		"          \"status\": \"generated\",\n" +
		"          \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><h5>Allergies And Intolerances</h5><table class=\\\"hapiPropertyTable\\\"><thead><tr><th>Allergen</th><th>Status</th><th>Category</th><th>Reaction</th><th>Severity</th><th>Comments</th><th>Onset</th></tr></thead><tbody><tr id=\\\"AllergyIntolerance-urn:uuid:1677b7b8-d04e-48cf-a859-6a3f7d8477ad\\\"><td> No information about allergies </td><td> active </td><td/><td/><td/><td/><td/></tr></tbody></table></div>\"\n" +
		"        },\n" +
		"        \"entry\": [ {\n" +
		"          \"reference\": \"urn:uuid:1677b7b8-d04e-48cf-a859-6a3f7d8477ad\"\n" +
		"        } ]\n" +
		"      }, {\n" +
		"        \"title\": \"Medication List\",\n" +
		"        \"code\": {\n" +
		"          \"coding\": [ {\n" +
		"            \"system\": \"http://loinc.org\",\n" +
		"            \"code\": \"10160-0\",\n" +
		"            \"display\": \"History of Medication use Narrative\"\n" +
		"          } ]\n" +
		"        },\n" +
		"        \"text\": {\n" +
		"          \"status\": \"generated\",\n" +
		"          \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><h5>Medication Summary: Medication Statements</h5><table class=\\\"hapiPropertyTable\\\"><thead><tr><th>Medication</th><th>Status</th><th>Route</th><th>Sig</th><th>Date</th></tr></thead><tbody><tr id=\\\"MedicationStatement-urn:uuid:603e6c86-3479-45ed-a233-39a829e3d8f4\\\"><td> No information about medications </td><td>Unknown</td><td/><td/><td/></tr></tbody></table></div>\"\n" +
		"        },\n" +
		"        \"entry\": [ {\n" +
		"          \"reference\": \"urn:uuid:603e6c86-3479-45ed-a233-39a829e3d8f4\"\n" +
		"        } ]\n" +
		"      }, {\n" +
		"        \"title\": \"Problem List\",\n" +
		"        \"code\": {\n" +
		"          \"coding\": [ {\n" +
		"            \"system\": \"http://loinc.org\",\n" +
		"            \"code\": \"11450-4\",\n" +
		"            \"display\": \"Problem list - Reported\"\n" +
		"          } ]\n" +
		"        },\n" +
		"        \"text\": {\n" +
		"          \"status\": \"generated\",\n" +
		"          \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><h5>Problem List</h5><table class=\\\"hapiPropertyTable\\\"><thead><tr><th>Medical Problems</th><th>Status</th><th>Comments</th><th>Onset Date</th></tr></thead><tbody><tr id=\\\"Condition-urn:uuid:a8742514-c8b0-483a-9a1a-169c6bb087c5\\\"><td> No information about problems </td><td> active </td><td/><td/></tr></tbody></table></div>\"\n" +
		"        },\n" +
		"        \"entry\": [ {\n" +
		"          \"reference\": \"urn:uuid:a8742514-c8b0-483a-9a1a-169c6bb087c5\"\n" +
		"        } ]\n" +
		"      }, {\n" +
		"        \"title\": \"History of Immunizations\",\n" +
		"        \"code\": {\n" +
		"          \"coding\": [ {\n" +
		"            \"system\": \"http://loinc.org\",\n" +
		"            \"code\": \"11369-6\",\n" +
		"            \"display\": \"History of Immunization Narrative\"\n" +
		"          } ]\n" +
		"        },\n" +
		"        \"text\": {\n" +
		"          \"status\": \"generated\",\n" +
		"          \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><h5>Immunizations</h5><table class=\\\"hapiPropertyTable\\\"><thead><tr><th>Immunization</th><th>Status</th><th>Dose Number</th><th>Manufacturer</th><th>Lot Number</th><th>Comments</th><th>Date</th></tr></thead><tbody><tr id=\\\"Immunization-http://localhost:8080/iis/fhir/l/Immunization/1659/_history/1\\\"><td/><td>COMPLETED</td><td/><td/><td>Y5841RR</td><td/><td> 2025-08-28T00:00:00-04:00 </td></tr></tbody></table></div>\"\n" +
		"        },\n" +
		"        \"entry\": [ {\n" +
		"          \"reference\": \"Immunization/1659\"\n" +
		"        } ]\n" +
		"      } ]\n" +
		"    }\n" +
		"  }, {\n" +
		"    \"fullUrl\": \"http://localhost:8080/iis/fhir/l/Patient/1653\",\n" +
		"    \"resource\": {\n" +
		"      \"resourceType\": \"Patient\",\n" +
		"      \"id\": \"1653\",\n" +
		"      \"meta\": {\n" +
		"        \"versionId\": \"1\",\n" +
		"        \"lastUpdated\": \"2025-08-28T16:32:54.593-04:00\"\n" +
		"      },\n" +
		"      \"text\": {\n" +
		"        \"status\": \"generated\",\n" +
		"        \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><div class=\\\"hapiHeaderText\\\">TalithaAIRA M <b>LILLEYAIRA </b></div><table class=\\\"hapiPropertyTable\\\"><tbody><tr><td>Identifier</td><td>J76A1</td></tr><tr><td>Address</td><td><span>1257 Schaal Ave </span><br/><span/><br/><span>Baroda </span><span>MI </span><span>USA </span></td></tr><tr><td>Date of birth</td><td><span>31 August 2021</span></td></tr></tbody></table></div>\"\n" +
		"      },\n" +
		"      \"extension\": [ {\n" +
		"        \"url\": \"recorded\",\n" +
		"        \"valueDate\": \"2025-08-28\"\n" +
		"      }, {\n" +
		"        \"url\": \"http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName\",\n" +
		"        \"valueString\": \"WayneAIRA\"\n" +
		"      }, {\n" +
		"        \"url\": \"http://hl7.org/fhir/us/core/StructureDefinition/us-core-race\",\n" +
		"        \"extension\": [ {\n" +
		"          \"url\": \"ombCategory\",\n" +
		"          \"valueCoding\": {\n" +
		"            \"system\": \"urn:oid:2.16.840.1.113883.6.238\",\n" +
		"            \"code\": \"2028-9\",\n" +
		"            \"display\": \"Asian\"\n" +
		"          }\n" +
		"        }, {\n" +
		"          \"url\": \"text\",\n" +
		"          \"valueString\": \"2028-9 \"\n" +
		"        } ]\n" +
		"      }, {\n" +
		"        \"url\": \"http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity\",\n" +
		"        \"extension\": [ {\n" +
		"          \"url\": \"ombCategory\",\n" +
		"          \"valueCoding\": {\n" +
		"            \"system\": \"urn:oid:2.16.840.1.113883.6.238\",\n" +
		"            \"code\": \"2186-5\",\n" +
		"            \"display\": \"not Hispanic or Latino\"\n" +
		"          }\n" +
		"        }, {\n" +
		"          \"url\": \"text\",\n" +
		"          \"valueString\": \"2186-5\"\n" +
		"        } ]\n" +
		"      }, {\n" +
		"        \"url\": \"publicity\",\n" +
		"        \"valueCoding\": {\n" +
		"          \"system\": \"http://terminology.hl7.org/ValueSet/v2-0215\",\n" +
		"          \"version\": \"Thu Aug 28 00:00:00 2025\",\n" +
		"          \"code\": \"02\"\n" +
		"        }\n" +
		"      }, {\n" +
		"        \"url\": \"registryStatus\",\n" +
		"        \"valueCoding\": {\n" +
		"          \"system\": \"http://terminology.hl7.org/ValueSet/v2-0441\",\n" +
		"          \"version\": \"Thu Aug 28 00:00:00 2025\",\n" +
		"          \"code\": \"A\"\n" +
		"        }\n" +
		"      } ],\n" +
		"      \"identifier\": [ {\n" +
		"        \"type\": {\n" +
		"          \"coding\": [ {\n" +
		"            \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0203\",\n" +
		"            \"code\": \"MR\"\n" +
		"          } ]\n" +
		"        },\n" +
		"        \"system\": \"AIRA-TEST\",\n" +
		"        \"value\": \"J76A1\"\n" +
		"      } ],\n" +
		"      \"name\": [ {\n" +
		"        \"extension\": [ {\n" +
		"          \"url\": \"v2-name-type\",\n" +
		"          \"valueCoding\": {\n" +
		"            \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0200\",\n" +
		"            \"code\": \"L\"\n" +
		"          }\n" +
		"        } ],\n" +
		"        \"family\": \"LilleyAIRA\",\n" +
		"        \"given\": [ \"TalithaAIRA\", \"M\" ]\n" +
		"      } ],\n" +
		"      \"telecom\": [ {\n" +
		"        \"extension\": [ {\n" +
		"          \"url\": \"use\",\n" +
		"          \"valueCoding\": {\n" +
		"            \"system\": \"http://terminology.hl7.org/ValueSet/v2-0201\",\n" +
		"            \"code\": \"PRN\"\n" +
		"          }\n" +
		"        } ],\n" +
		"        \"system\": \"phone\",\n" +
		"        \"value\": \"2698506008\",\n" +
		"        \"use\": \"home\"\n" +
		"      }, {\n" +
		"        \"system\": \"email\"\n" +
		"      } ],\n" +
		"      \"gender\": \"female\",\n" +
		"      \"birthDate\": \"2021-08-31\",\n" +
		"      \"address\": [ {\n" +
		"        \"line\": [ \"1257 Schaal Ave\" ],\n" +
		"        \"city\": \"Baroda\",\n" +
		"        \"state\": \"MI\",\n" +
		"        \"postalCode\": \"49101\",\n" +
		"        \"country\": \"USA\"\n" +
		"      } ],\n" +
		"      \"contact\": [ {\n" +
		"        \"relationship\": [ {\n" +
		"          \"coding\": [ {\n" +
		"            \"code\": \"MTH\",\n" +
		"            \"display\": \"Mother\"\n" +
		"          } ],\n" +
		"          \"text\": \"MTH\"\n" +
		"        } ],\n" +
		"        \"name\": {\n" +
		"          \"extension\": [ {\n" +
		"            \"url\": \"v2-name-type\",\n" +
		"            \"valueCoding\": {\n" +
		"              \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0200\"\n" +
		"            }\n" +
		"          } ],\n" +
		"          \"family\": \"WayneAIRA\",\n" +
		"          \"given\": [ \"HarperAIRA\" ]\n" +
		"        }\n" +
		"      } ]\n" +
		"    }\n" +
		"  }, {\n" +
		"    \"fullUrl\": \"urn:uuid:1677b7b8-d04e-48cf-a859-6a3f7d8477ad\",\n" +
		"    \"resource\": {\n" +
		"      \"resourceType\": \"AllergyIntolerance\",\n" +
		"      \"extension\": [ {\n" +
		"        \"url\": \"http://hl7.org/fhir/StructureDefinition/narrativeLink\",\n" +
		"        \"valueUrl\": \"urn:uuid:ef3d0efb-7290-4576-b353-1f0de7c668e8#AllergyIntolerance-urn:uuid:1677b7b8-d04e-48cf-a859-6a3f7d8477ad\"\n" +
		"      } ],\n" +
		"      \"clinicalStatus\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical\",\n" +
		"          \"code\": \"active\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"code\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"http://hl7.org/fhir/uv/ips/CodeSystem/absent-unknown-uv-ips\",\n" +
		"          \"code\": \"no-allergy-info\",\n" +
		"          \"display\": \"No information about allergies\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"patient\": {\n" +
		"        \"reference\": \"Patient/1653\"\n" +
		"      }\n" +
		"    }\n" +
		"  }, {\n" +
		"    \"fullUrl\": \"urn:uuid:603e6c86-3479-45ed-a233-39a829e3d8f4\",\n" +
		"    \"resource\": {\n" +
		"      \"resourceType\": \"MedicationStatement\",\n" +
		"      \"extension\": [ {\n" +
		"        \"url\": \"http://hl7.org/fhir/StructureDefinition/narrativeLink\",\n" +
		"        \"valueUrl\": \"urn:uuid:ef3d0efb-7290-4576-b353-1f0de7c668e8#MedicationStatement-urn:uuid:603e6c86-3479-45ed-a233-39a829e3d8f4\"\n" +
		"      } ],\n" +
		"      \"status\": \"unknown\",\n" +
		"      \"medicationCodeableConcept\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"http://hl7.org/fhir/uv/ips/CodeSystem/absent-unknown-uv-ips\",\n" +
		"          \"code\": \"no-medication-info\",\n" +
		"          \"display\": \"No information about medications\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"subject\": {\n" +
		"        \"reference\": \"Patient/1653\"\n" +
		"      }\n" +
		"    }\n" +
		"  }, {\n" +
		"    \"fullUrl\": \"urn:uuid:a8742514-c8b0-483a-9a1a-169c6bb087c5\",\n" +
		"    \"resource\": {\n" +
		"      \"resourceType\": \"Condition\",\n" +
		"      \"extension\": [ {\n" +
		"        \"url\": \"http://hl7.org/fhir/StructureDefinition/narrativeLink\",\n" +
		"        \"valueUrl\": \"urn:uuid:ef3d0efb-7290-4576-b353-1f0de7c668e8#Condition-urn:uuid:a8742514-c8b0-483a-9a1a-169c6bb087c5\"\n" +
		"      } ],\n" +
		"      \"clinicalStatus\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"http://terminology.hl7.org/CodeSystem/condition-clinical\",\n" +
		"          \"code\": \"active\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"code\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"http://hl7.org/fhir/uv/ips/CodeSystem/absent-unknown-uv-ips\",\n" +
		"          \"code\": \"no-problem-info\",\n" +
		"          \"display\": \"No information about problems\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"subject\": {\n" +
		"        \"reference\": \"Patient/1653\"\n" +
		"      }\n" +
		"    }\n" +
		"  }, {\n" +
		"    \"fullUrl\": \"http://localhost:8080/iis/fhir/l/Immunization/1659\",\n" +
		"    \"resource\": {\n" +
		"      \"resourceType\": \"Immunization\",\n" +
		"      \"id\": \"1659\",\n" +
		"      \"meta\": {\n" +
		"        \"versionId\": \"1\",\n" +
		"        \"lastUpdated\": \"2025-08-28T16:32:56.235-04:00\",\n" +
		"        \"tag\": [ {\n" +
		"          \"system\": \"http://hapifhir.io/fhir/NamingSystem/mdm-record-status\",\n" +
		"          \"version\": \"1\",\n" +
		"          \"code\": \"GOLDEN_RECORD\",\n" +
		"          \"display\": \"Golden Record\",\n" +
		"          \"userSelected\": false\n" +
		"        }, {\n" +
		"          \"system\": \"https://hapifhir.org/NamingSystem/managing-mdm-system\",\n" +
		"          \"version\": \"1\",\n" +
		"          \"code\": \"HAPI-MDM\",\n" +
		"          \"display\": \"This Golden Resource can only be modified by HAPI MDM system.\",\n" +
		"          \"userSelected\": false\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"extension\": [ {\n" +
		"        \"url\": \"actionCode\",\n" +
		"        \"valueCoding\": {\n" +
		"          \"system\": \"0206\",\n" +
		"          \"code\": \"A\"\n" +
		"        }\n" +
		"      }, {\n" +
		"        \"url\": \"completionStatus\",\n" +
		"        \"valueCoding\": {\n" +
		"          \"system\": \"0322\",\n" +
		"          \"code\": \"CP\"\n" +
		"        }\n" +
		"      }, {\n" +
		"        \"url\": \"http://hl7.org/fhir/StructureDefinition/narrativeLink\",\n" +
		"        \"valueUrl\": \"urn:uuid:ef3d0efb-7290-4576-b353-1f0de7c668e8#Immunization-http://localhost:8080/iis/fhir/l/Immunization/1659/_history/1\"\n" +
		"      } ],\n" +
		"      \"identifier\": [ {\n" +
		"        \"system\": \"http://hapifhir.io/fhir/NamingSystem/mdm-golden-resource-enterprise-id\",\n" +
		"        \"value\": \"9c59b883-fdb2-4cf6-8f19-9c3c411775c5\"\n" +
		"      }, {\n" +
		"        \"type\": {\n" +
		"          \"coding\": [ {\n" +
		"            \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0203\",\n" +
		"            \"code\": \"FILL\"\n" +
		"          } ]\n" +
		"        },\n" +
		"        \"system\": \"AIRA\",\n" +
		"        \"value\": \"J76A1.3\"\n" +
		"      } ],\n" +
		"      \"status\": \"completed\",\n" +
		"      \"statusReason\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"refusalReasonCode\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"vaccineCode\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"http://hl7.org/fhir/sid/cvx\",\n" +
		"          \"code\": \"21\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"patient\": {\n" +
		"        \"reference\": \"Patient/1653\"\n" +
		"      },\n" +
		"      \"occurrenceDateTime\": \"2025-08-28T00:00:00-04:00\",\n" +
		"      \"recorded\": \"2025-08-28T16:32:54-04:00\",\n" +
		"      \"reportOrigin\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"NIP001\",\n" +
		"          \"code\": \"00\",\n" +
		"          \"display\": \"New immunization record\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"manufacturer\": {\n" +
		"        \"identifier\": {\n" +
		"          \"system\": \"http://terminology.hl7.org/CodeSystem/MVX\",\n" +
		"          \"value\": \"MSD\"\n" +
		"        }\n" +
		"      },\n" +
		"      \"lotNumber\": \"Y5841RR\",\n" +
		"      \"site\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"http://hl7.org/fhir/ValueSet/immunization-site\",\n" +
		"          \"code\": \"RA\",\n" +
		"          \"display\": \"Right Upper Arm\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"route\": {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"http://hl7.org/fhir/ValueSet/immunization-route\",\n" +
		"          \"code\": \"C38299\",\n" +
		"          \"display\": \"Subcutaneous\"\n" +
		"        } ]\n" +
		"      },\n" +
		"      \"doseQuantity\": {\n" +
		"        \"value\": 0.5\n" +
		"      },\n" +
		"      \"performer\": [ {\n" +
		"        \"function\": {\n" +
		"          \"coding\": [ {\n" +
		"            \"system\": \"http://terminology.hl7.org/CodeSystem/v2-0443\",\n" +
		"            \"code\": \"EP\",\n" +
		"            \"display\": \"Entering Provider\"\n" +
		"          } ]\n" +
		"        },\n" +
		"        \"actor\": {\n" +
		"          \"type\": \"Practitioner\",\n" +
		"          \"identifier\": {\n" +
		"            \"system\": \"PRN\",\n" +
		"            \"value\": \"I-23432\"\n" +
		"          }\n" +
		"        }\n" +
		"      } ],\n" +
		"      \"programEligibility\": [ {\n" +
		"        \"coding\": [ {\n" +
		"          \"system\": \"http://hl7.org/fhir/ValueSet/immunization-program-eligibility\",\n" +
		"          \"code\": \"V01\",\n" +
		"          \"display\": \"Not VFC eligible\"\n" +
		"        } ]\n" +
		"      } ]\n" +
		"    }\n" +
		"  }, {\n" +
		"    \"resource\": {\n" +
		"      \"resourceType\": \"Organization\",\n" +
		"      \"identifier\": [ {\n" +
		"        \"system\": \"Tenant\",\n" +
		"        \"value\": \"1\"\n" +
		"      } ],\n" +
		"      \"name\": \"l\"\n" +
		"    }\n" +
		"  } ]\n" +
		"}";

	private static final String EXAMPLE_JWK = "{\n" +
		"  \"alg\": \"ESP256\",\n" +
		"  \"kty\": \"EC\",\n" +
		"  \"d\": \"k8mbFqMBPvRwNuZwPMASgZeVsdo64moTWp396XZL8NY\",\n" +
		"  \"use\": \"sig\",\n" +
		"  \"crv\": \"P-256\",\n" +
		"  \"kid\": \"be6b28b8-f5a4-4543-a80a-bc764c619feb\",\n" +
		"  \"x\": \"OdOP_Dx0ty9qmM9nq7kZmggWprTeOUtb2eCO7fqCvbY\",\n" +
		"  \"y\": \"yyQNgpnaAKeRshZJ657tBqTIBvT4JCMa7WgCxuGZhZM\"\n" +
		"}";

	private final NUVAService nuvaService;
	private final CLVRService clvrService;
	private final SigningService signingService;
	private final CborService cborService;
	private final QrCodeService qrCodeService;
	private final FhirConversionUtil fhirConversionUtil;
	private final CLVRPdfService clvrPdfService;
	private final FhirContext fhirContext;
	private final IIpsGeneratorSvc ipsGeneratorSvc;
	private final RequestTenantUtil requestTenantUtil;

	@Autowired
	public CLVRTestRestController(
		NUVAService nuvaService,
		CLVRService clvrService,
		SigningService signingService,
		CborService cborService,
		QrCodeService qrCodeService,
		FhirConversionUtil fhirConversionUtil,
		CLVRPdfService clvrPdfService,
		FhirContext fhirContext,
		IIpsGeneratorSvc ipsGeneratorSvc,
		RequestTenantUtil requestTenantUtil) {
		this.nuvaService = nuvaService;
		this.clvrService = clvrService;
		this.signingService = signingService;
		this.cborService = cborService;
		this.qrCodeService = qrCodeService;
		this.fhirConversionUtil = fhirConversionUtil;
		this.clvrPdfService = clvrPdfService;
		this.fhirContext = fhirContext;
		this.ipsGeneratorSvc = ipsGeneratorSvc;
		this.requestTenantUtil = requestTenantUtil;
	}

	// --- 1. Key Operations ---

	@GetMapping("/example-key")
	public ResponseEntity<Map<String, String>> getExampleKey() {
		return ResponseEntity.ok(Collections.singletonMap("jwk", EXAMPLE_JWK));
	}

	@PostMapping("/load-key")
	public ResponseEntity<Map<String, String>> loadKeyPair(@RequestBody Map<String, String> request) {
		try {
			String jwkString = request.get("jwk");
			JWK jwk = JWK.parse(jwkString);
			String kid = jwk.getKeyID();
			return ResponseEntity.ok(Map.of(
				"message", "Key stored and ready to use",
				"kid", kid != null ? kid : ""
			));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", "Key pair could not be loaded: " + ex.getMessage()));
		}
	}

	// --- 2. Bundle & Token Operations ---

	@GetMapping("/example-fhir")
	public ResponseEntity<Map<String, String>> getExampleFhir(
		@RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant
//		@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId
	) {
//		IBaseBundle iBaseBundle = ipsGeneratorSvc.generateIps(
//			requestTenantUtil.requestDetailsWithPartitionName(tenant),
//			new IdType(patientId),
//			""
//		);
//		String bundleString = fhirContext.newJsonParser().encodeResourceToString(iBaseBundle);
		return ResponseEntity.ok(Map.of(
			"issuer", "SYA",
			"fhirBundle", IPS_SAMPLE_R4_IIS
		));
	}

	@PostMapping("/convert-fhir")
	public ResponseEntity<Map<String, Object>> convertFhirBundle(@RequestBody ConvertFhirRequest request) {
		try {
			if (request.getFhirBundle() == null || request.getFhirBundle().isBlank()) {
				throw new IllegalArgumentException("FHIR Bundle cannot be empty!");
			}
			Bundle fhirBundle = fhirContext.newJsonParser()
				.setPrettyPrint(true)
				.parseResource(Bundle.class, request.getFhirBundle().trim());

			CLVRPayload clvrPayloadFromBundle = fhirConversionUtil.toCLVRPayloadFromBundle(fhirBundle);
			CLVRToken clvrToken = new CLVRToken(clvrPayloadFromBundle, request.getIssuer().trim());

			return ResponseEntity.ok(Map.of("clvrToken", clvrToken));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", "Failed to convert FHIR bundle: " + ex.getMessage()));
		}
	}

	@PostMapping("/sign-and-compress")
	public ResponseEntity<Map<String, String>> signAndCompress(@RequestBody SignCompressRequest request) {
		try {
			CLVRToken clvrToken = CLVRToken.fromString(request.getClvrTokenJson().trim());
			JWK jwk = JWK.parse(request.getJwk().trim());
			KeyPair keyPair = jwk.toECKey().toKeyPair();
			String kid = jwk.getKeyID();

			String qrCodeString = clvrService.encodeCLVRtoQrCode(clvrToken, keyPair, kid);
			return ResponseEntity.ok(Map.of("qrCodeString", qrCodeString));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", "Failed to sign and compress token: " + ex.getMessage()));
		}
	}

	// --- 3. QR & Signature Operations ---

	@PostMapping("/parse-qr")
	public ResponseEntity<Map<String, Object>> parseQrToToken(@RequestBody ParseQrRequest request) {
		try {
			JWK jwk = JWK.parse(request.getJwk().trim());
			ECKey ecKey = jwk.toECKey();

			Map<String, PublicKey> publicKeyMap = new HashMap<>();
			publicKeyMap.put(jwk.getKeyID(), ecKey.toPublicKey());

			CLVRToken clvrToken = clvrService.decodeFullQrCode(request.getQrCodeString().trim().getBytes(), publicKeyMap);
			return ResponseEntity.ok(Map.of("clvrTokenPretty", clvrToken.toPrettyString()));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", "Failed to parse QR code: " + ex.getMessage()));
		}
	}

	@PostMapping("/check-signature")
	public ResponseEntity<Map<String, Object>> checkSignature(@RequestBody ParseQrRequest request) {
		try {
			JWK jwk = JWK.parse(request.getJwk().trim());
			ECKey ecKey = jwk.toECKey();

			Map<String, PublicKey> publicKeyMap = new HashMap<>();
			publicKeyMap.put(jwk.getKeyID(), ecKey.toPublicKey());

			clvrService.decodeFullQrCode(request.getQrCodeString().trim().getBytes(), publicKeyMap);
			return ResponseEntity.ok(Map.of(
				"valid", true,
				"message", "Signature validated by provided key"
			));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
				"valid", false,
				"message", "Verification failed: " + ex.getMessage()
			));
		}
	}

	// --- 4. Render & PDF Outputs ---

	@PostMapping(value = "/generate-qr-image", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<?> generateQrImage(@RequestBody Map<String, String> request) {
		try {
			String qrText = request.get("qrCodeString");
			if (qrText == null || qrText.isBlank()) {
				return ResponseEntity.badRequest().body(Map.of("error", "qrCodeString is required"));
			}
			BitMatrix matrix = new MultiFormatWriter().encode(qrText.trim(), BarcodeFormat.QR_CODE, 300, 300);
			BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);

			return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.body(baos.toByteArray());
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", "Failed to generate QR image: " + ex.getMessage()));
		}
	}

	@PostMapping(value = "/render-pdf-image", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> renderPdfImage(@RequestBody PdfRequest request) {
		try {
			CLVRToken clvrToken = CLVRToken.fromString(request.getClvrTokenJson().trim());
			try (PDDocument pdDocument = clvrPdfService.createPdf(clvrToken, request.getQrCodeString().trim().getBytes(), "REST-API")) {
				PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
				BufferedImage image = pdfRenderer.renderImageWithDPI(0, 75, ImageType.RGB);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "png", baos);

				return ResponseEntity.ok()
					.contentType(MediaType.IMAGE_PNG)
					.body(baos.toByteArray());
			}
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", "Failed to render PDF image: " + ex.getMessage()));
		}
	}

	@PostMapping(value = "/export-pdf", produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> exportPdf(@RequestBody PdfRequest request) {
		try {
			CLVRToken clvrToken = CLVRToken.fromString(request.getClvrTokenJson().trim());
			try (PDDocument pdDocument = clvrPdfService.createPdf(clvrToken, request.getQrCodeString().trim().getBytes(), "REST-API")) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				pdDocument.save(baos);

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_PDF);
				headers.setContentDispositionFormData("attachment", "ips-to-clvr-export.pdf");

				return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
			}
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", "Failed to export PDF: " + ex.getMessage()));
		}
	}

	// --- DTO Helper Classes ---

	public static class ConvertFhirRequest {
		private String issuer;
		private String fhirBundle;

		public String getIssuer() {
			return issuer;
		}

		public void setIssuer(String issuer) {
			this.issuer = issuer;
		}

		public String getFhirBundle() {
			return fhirBundle;
		}

		public void setFhirBundle(String fhirBundle) {
			this.fhirBundle = fhirBundle;
		}
	}

	public static class SignCompressRequest {
		private String clvrTokenJson;
		private String jwk;

		public String getClvrTokenJson() {
			return clvrTokenJson;
		}

		public void setClvrTokenJson(String clvrTokenJson) {
			this.clvrTokenJson = clvrTokenJson;
		}

		public String getJwk() {
			return jwk;
		}

		public void setJwk(String jwk) {
			this.jwk = jwk;
		}
	}

	public static class ParseQrRequest {
		private String qrCodeString;
		private String jwk;

		public String getQrCodeString() {
			return qrCodeString;
		}

		public void setQrCodeString(String qrCodeString) {
			this.qrCodeString = qrCodeString;
		}

		public String getJwk() {
			return jwk;
		}

		public void setJwk(String jwk) {
			this.jwk = jwk;
		}
	}

	public static class PdfRequest {
		private String clvrTokenJson;
		private String qrCodeString;

		public String getClvrTokenJson() {
			return clvrTokenJson;
		}

		public void setClvrTokenJson(String clvrTokenJson) {
			this.clvrTokenJson = clvrTokenJson;
		}

		public String getQrCodeString() {
			return qrCodeString;
		}

		public void setQrCodeString(String qrCodeString) {
			this.qrCodeString = qrCodeString;
		}
	}
}