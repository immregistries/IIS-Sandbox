package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.persisted.model.ShLinkManifest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShlinkManifestRepository extends JpaRepository<ShLinkManifest, String> {
}
