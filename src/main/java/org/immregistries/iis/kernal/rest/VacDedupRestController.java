package org.immregistries.iis.kernal.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.immregistries.iis.kernal.fhir.security.CurrentTenantUtil;
import org.immregistries.iis.kernal.fhir.security.TenantUtil;
import org.immregistries.iis.kernal.model.persisted.Tenant;
import org.immregistries.vaccination_deduplication.Immunization;
import org.immregistries.vaccination_deduplication.LinkedImmunization;
import org.immregistries.vaccination_deduplication.VaccinationDeduplication;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/rest/tenant/{tenantId}/vacDedup")
public class VacDedupRestController {

    public static final String ALGORITHM_DETERMINISTIC = "Deterministic";
    public static final String ALGORITHM_WEIGHTED = "Weighted";
    public static final String ALGORITHM_HYBRID = "Hybrid";

    @PostMapping
    public List<LinkedImmunization> deduplicate(
            @PathVariable int tenantId,
            @RequestBody VacDedupRequest request,
            HttpServletRequest req) {

        try (org.hibernate.Session dataSession = org.immregistries.iis.kernal.HibernateConfig.getDataSession()) {
            Tenant tenant = TenantUtil.getTenantByIdAuthenticated(tenantId, dataSession);
            if (tenant == null) {
                throw new RuntimeException("Access is not authorized");
            }
            CurrentTenantUtil.getTenant(tenant.getOrganizationName(), req, dataSession);

            LinkedImmunization immunizationList = new LinkedImmunization();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

            if (request.getImmunizations() != null) {
                for (VacDedupRequest.ImmunizationItem item : request.getImmunizations()) {
                    Date date = null;
                    try {
                        if (item.getDate() != null && !item.getDate().isEmpty()) {
                            date = sdf.parse(item.getDate());
                        }
                    } catch (ParseException pe) {
                        // Ignore or handle error
                    }

                    if (date != null && item.getCvx() != null && !item.getCvx().isEmpty()) {
                        Immunization immunization = new Immunization();
                        immunization.setCVX(item.getCvx());
                        immunization.setDate(date);
                        immunization.setMVX(item.getMvx());
                        immunization.setLotNumber(item.getLot());
                        immunization.setOrganisationID(item.getOrg());

                        ImmunizationSource source = ImmunizationSource.HISTORICAL;
                        if (item.getSource() != null && !item.getSource().isEmpty()) {
                            try {
                                source = ImmunizationSource.valueOf(item.getSource());
                            } catch (IllegalArgumentException e) {
                                // default to HISTORICAL
                            }
                        }
                        immunization.setSource(source);
                        immunizationList.add(immunization);
                    }
                }
            }

            ArrayList<LinkedImmunization> immunizationListResults = new ArrayList<>();
            if (immunizationList.size() > 1) {
                VaccinationDeduplication vaccinationDeduplication = new VaccinationDeduplication();
                String algorithm = request.getAlgorithm();
                if (algorithm == null) {
                    algorithm = ALGORITHM_DETERMINISTIC;
                }

                if (algorithm.equals(ALGORITHM_DETERMINISTIC)) {
                    immunizationListResults = vaccinationDeduplication.deduplicateDeterministic(immunizationList);
                } else if (algorithm.equals(ALGORITHM_WEIGHTED)) {
                    immunizationListResults = vaccinationDeduplication.deduplicateWeighted(immunizationList);
                } else if (algorithm.equals(ALGORITHM_HYBRID)) {
                    immunizationListResults = vaccinationDeduplication.deduplicateHybrid(immunizationList);
                }
            }
            return immunizationListResults;
        }
    }

    public static class VacDedupRequest {
        private String algorithm;
        private List<ImmunizationItem> immunizations;

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public List<ImmunizationItem> getImmunizations() {
            return immunizations;
        }

        public void setImmunizations(List<ImmunizationItem> immunizations) {
            this.immunizations = immunizations;
        }

        public static class ImmunizationItem {
            private String date;
            private String cvx;
            private String mvx;
            private String lot;
            private String org;
            private String source;

            public String getDate() {
                return date;
            }

            public void setDate(String date) {
                this.date = date;
            }

            public String getCvx() {
                return cvx;
            }

            public void setCvx(String cvx) {
                this.cvx = cvx;
            }

            public String getMvx() {
                return mvx;
            }

            public void setMvx(String mvx) {
                this.mvx = mvx;
            }

            public String getLot() {
                return lot;
            }

            public void setLot(String lot) {
                this.lot = lot;
            }

            public String getOrg() {
                return org;
            }

            public void setOrg(String org) {
                this.org = org;
            }

            public String getSource() {
                return source;
            }

            public void setSource(String source) {
                this.source = source;
            }
        }
    }
}
