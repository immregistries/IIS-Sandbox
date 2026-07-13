package org.immregistries.iis.kernal.logic.shlink;

import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.persisted.entities.ShLinkManifest;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.persisted.repository.ShlinkManifestRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
/**
 * Service to manipulate stored Smart Health Link Manifests
 */
public class ShLinkManifestStoreService {

	@Autowired
	private ShlinkManifestRepository shlinkManifestRepository;

	public ShLinkManifest saveManifest(ShLinkManifest shLinkManifest) {
		if (StringUtils.isBlank(shLinkManifest.getId())) {
			shLinkManifest.setId(UUID.randomUUID().toString());
		}
		return shlinkManifestRepository.save(shLinkManifest);
	}

	public ShLinkManifest readManifest(String manifestId) {
		return shlinkManifestRepository.findById(manifestId).orElse(null);
	}

	public @NotNull List<ShLinkManifest> getAllManifests() {
		return shlinkManifestRepository.findAll();
	}

	public @NotNull List<ShLinkManifest> getAllPatientManifests(Tenant tenant, String patientId) {
		return shlinkManifestRepository.findByTenantAndPatientId(tenant, patientId);
	}

	public @NotNull List<ShLinkManifest> getAllTenantManifests(Tenant tenant) {
		return shlinkManifestRepository.findByTenant(tenant);
	}


}
