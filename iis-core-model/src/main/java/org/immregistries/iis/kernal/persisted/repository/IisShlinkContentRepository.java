package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.persisted.entities.IisShLinkContent;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IisShlinkContentRepository extends JpaRepository<IisShLinkContent, Integer> {

	List<IisShLinkContent> findByTenantAndPatientId(Tenant tenant, String patientId);
}
