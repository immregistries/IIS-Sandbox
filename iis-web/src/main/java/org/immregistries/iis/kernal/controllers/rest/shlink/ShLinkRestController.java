package org.immregistries.iis.kernal.controllers.rest.shlink;

import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisPathVariable;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.controllers.request.shlink.ShLinkCreationRequestDTO;
import org.immregistries.iis.kernal.logic.shlink.generation.ShLinkGenerator;
import org.immregistries.iis.kernal.persisted.entities.ShLinkGenerated;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.entities.UserAccess;
import org.immregistries.iis.kernal.persisted.repository.ShLinkGeneratedRepository;
import org.immregistries.iis.kernal.services.QrCodeEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.immregistries.iis.kernal.controllers.IisRestPath.BasePath.SH_LINK_PATH;
import static org.immregistries.iis.kernal.controllers.IisRestPath.REST_PATIENT_PATH;

@RestController
@RequestMapping()
public class ShLinkRestController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private ShLinkGenerator shLinkGenerator;
	@Autowired
	private QrCodeEncoder qrCodeEncoder;
	@Autowired
	private ShLinkGeneratedRepository shLinkGeneratedRepository;

	@PostMapping({IisRestPath.BasePath.REST_PATH + SH_LINK_PATH, IisRestPath.REST_TENANT_PATH + SH_LINK_PATH})
	public String createShLinkIPSQrCode(
		@AuthenticationPrincipal UserAccess userAccess,
		HttpServletRequest req,
		ShLinkCreationRequestDTO dto,
		@RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant)
			throws IOException, NoSuchAlgorithmException {
		String keyId = dto.getKeyId();
		String secretKey = dto.getSecretKey();
		String patientId = dto.getPatientId();
		String flag = dto.getFlag();
		String passcode = dto.getPasscode();
		String exp = dto.getExp();
		ServletUriComponentsBuilder uriBuilder = ServletUriComponentsBuilder.fromRequest(req);
		String label = dto.getLabel();
		String description = dto.getDescription();
		ShLinkGenerated shLinkGenerated = shLinkGenerator.generateShLink(keyId, secretKey, patientId, flag, exp, tenant, userAccess, uriBuilder, passcode, label, description);
		return shLinkGenerated.getEncodedQR();
	}

	@GetMapping({IisRestPath.REST_TENANT_PATH + SH_LINK_PATH})
	public List<ShLinkGenerated> listShLinks(
		@RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant) {
		return shLinkGeneratedRepository.findByTenant(tenant);
	}

	@GetMapping({REST_PATIENT_PATH + SH_LINK_PATH})
	public List<ShLinkGenerated> listShLinksByPatientId(
		@RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		@PathVariable(IisPathVariable.Key.PATIENT_ID) String patientId) {
		return shLinkGeneratedRepository.findByTenantAndPatientId(tenant, patientId);
	}

	@GetMapping({IisRestPath.REST_TENANT_PATH + SH_LINK_PATH + "/{shLinkId}"})
	public ResponseEntity<ShLinkGenerated> getShLinkById(
		@RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		@PathVariable("shLinkId") String shLinkId) {
		return shLinkGeneratedRepository.findById(shLinkId)
			.filter(shLink -> shLink.getTenant().getOrgId() == tenant.getOrgId())
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping({IisRestPath.REST_TENANT_PATH + SH_LINK_PATH + "/{shLinkId}"})
	public ResponseEntity<Void> deleteShLink(
		@RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant,
		@PathVariable("shLinkId") String shLinkId) {
		return shLinkGeneratedRepository.findById(shLinkId)
			.filter(shLink -> shLink.getTenant().getOrgId() == tenant.getOrgId())
			.map(shLink -> {
				shLinkGeneratedRepository.delete(shLink);
				return ResponseEntity.ok().<Void>build();
			})
			.orElse(ResponseEntity.notFound().build());
	}
}
