package org.immregistries.iis.kernal.persisted.repository;

import org.immregistries.iis.kernal.persisted.model.MessageReceived;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageReceivedRepository extends JpaRepository<MessageReceived, Integer> {
    List<MessageReceived> findByTenantOrderByReportedDateDesc(Tenant tenant);

    List<MessageReceived> findByPatientReportedId(String patientId);

    List<MessageReceived> findByTenantAndPatientReportedId(Tenant tenant, String patientId);
}
