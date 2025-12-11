package org.immregistries.iis.kernal.persisted.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReceivedRepository extends JpaRepository<MessageReceived, Integer> {
    List<MessageReceived> findByTenantOrderByReportedDateDesc(Tenant tenant);

    List<MessageReceived> findByPatientReportedId(String patientId);
}
