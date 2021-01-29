package org.immregistries.iis.kernal.repository;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.PatientReported;

public class PatientRepository {

  public static PatientReported getPatientFromExternalId(OrgAccess orgAccess, Session dataSession,
      String patientReportedExternalLink) {
    PatientReported patientReported = null;

    Query query = dataSession.createQuery(
        "from PatientReported where orgReported = ? and patientReportedExternalLink = ?");
    query.setParameter(0, orgAccess.getOrg());
    query.setParameter(1, patientReportedExternalLink);
    @SuppressWarnings("unchecked")
	List<PatientReported> patientReportedList = query.list();
    if (patientReportedList.size() > 0) {
      patientReported = patientReportedList.get(0);
    }
    return patientReported;
  }
}
