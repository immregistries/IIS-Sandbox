package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.patch.FhirPatch;
import ca.uhn.fhir.jpa.provider.DiffProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.base.Objects;
import org.hl7.fhir.instance.model.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Comparator;

@Service
/**
 * NOT USED FOR DIFF OPERATION
 * based on Hapi DiffProvider
 */
public class CrossTenantDiffProvider {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	FhirContext myContext;
	@Autowired
	private DaoRegistry myDaoRegistry;

	public IBaseParameters diff(
		IIdType theFromVersion,
		IIdType theToVersion,
		IPrimitiveType<Boolean> theIncludeMeta,
		RequestDetails theRequestDetails) {

		if (!Objects.equal(theFromVersion.getResourceType(), theToVersion.getResourceType())) {
			String msg = myContext.getLocalizer().getMessage(DiffProvider.class, "cantDiffDifferentTypes");
			throw new InvalidRequestException(Msg.code(1129) + msg);
		}

		IFhirResourceDao<?> dao = myDaoRegistry.getResourceDao(theFromVersion.getResourceType());
		IBaseResource sourceResource = dao.read(theFromVersion, theRequestDetails);
		IBaseResource targetResource = dao.read(theToVersion, theRequestDetails);

		try {
			IDomainResource sourceDomain = (IDomainResource) sourceResource;
			IDomainResource targetDomain = (IDomainResource) targetResource;
			if (targetDomain.hasExtension() && sourceDomain.hasExtension()) {
				sourceDomain.getExtension().sort(extensionUrlSimilarityComparator(targetDomain));
				targetDomain.getExtension().sort(extensionUrlSimilarityComparator(sourceDomain));
			}
		} catch (ClassCastException ignored) {
		}

		FhirPatch fhirPatch = newPatch(theIncludeMeta);
		return fhirPatch.diff(sourceResource, targetResource);
	}



	@Nonnull
	public FhirPatch newPatch(IPrimitiveType<Boolean> theIncludeMeta) {
		FhirPatch fhirPatch = new CrossTenantFhirPatch(myContext);
		fhirPatch.setIncludePreviousValueInDiff(true);
		if (theIncludeMeta != null && (Boolean) theIncludeMeta.getValue()) {
			logger.trace("Including resource metadata in patch");
		} else {
			fhirPatch.addIgnorePath("*.meta");
		}

		fhirPatch.addIgnorePath("*.text.div");
		fhirPatch.addIgnorePath("*.id");
		fhirPatch.addIgnorePath("*.reference");
		fhirPatch.addIgnorePath("Immunization.recorded");

		return fhirPatch;
	}

	/**
	 * Sorting extensions to harmonize position by ordering through Similarity then Url
	 */
	public static Comparator<IBaseExtension<?, ?>> extensionUrlSimilarityComparator(IBaseHasExtensions otherResource) {
		return (e1, e2) -> {
			boolean targetHas1 = otherResource.getExtension().stream().anyMatch(e -> e.getUrl().equals(e1.getUrl()));
			boolean targetHas2 = otherResource.getExtension().stream().anyMatch(e -> e.getUrl().equals(e2.getUrl()));
			if (targetHas1 == targetHas2) {
				return e1.getUrl().compareTo(e2.getUrl());
			} else if (targetHas1) {
				return -1;
			} else {
				return 1;
			}
		};
	}
}
