package org.immregistries.iis.kernal.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.patch.FhirPatch;
import ca.uhn.fhir.jpa.provider.DiffProvider;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import com.google.common.base.Objects;
import org.hl7.fhir.instance.model.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Comparator;

@Service
public class CustomDiffProvider extends DiffProvider {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	FhirContext myContext;
	@Autowired
	private DaoRegistry myDaoRegistry;


	@Override
	@Description(
		"This operation examines two resource versions (can be two versions of the same resource, or two different resources) and generates a FHIR Patch document showing the differences.")
	@Operation(name = ProviderConstants.DIFF_OPERATION_NAME, idempotent = true)
	public IBaseParameters diff(
		@Description(value = "The resource ID and version to diff from", example = "Patient/example/version/1")
		@OperationParam(name = ProviderConstants.DIFF_FROM_PARAMETER, typeName = "id", min = 1, max = 1)
		IIdType theFromVersion,
		@Description(value = "The resource ID and version to diff to", example = "Patient/example/version/2")
		@OperationParam(name = ProviderConstants.DIFF_TO_PARAMETER, typeName = "id", min = 1, max = 1)
		IIdType theToVersion,
		@Description(
			value = "Should differences in the Resource.meta element be included in the diff",
			example = "false")
		@OperationParam(
			name = ProviderConstants.DIFF_INCLUDE_META_PARAMETER,
			typeName = "boolean",
			min = 0,
			max = 1)
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
			logger.info("test1 {} {}", sourceDomain.getExtension().size(), targetDomain.getExtension().size());

			if (targetDomain.hasExtension() && sourceDomain.hasExtension()) {
				sourceDomain.getExtension().remove(0);
//					.sort(extensionUrlSimilarityComparator(targetDomain));
				targetDomain.getExtension().sort(extensionUrlSimilarityComparator(sourceDomain));
			}
			logger.info("test2 {} {}", sourceDomain.getExtension().size(), targetDomain.getExtension().size());

		} catch (ClassCastException ignored) {
			logger.info("testWESH");

		}

		FhirPatch fhirPatch = newPatch(theIncludeMeta);
		return fhirPatch.diff(sourceResource, targetResource);
	}



	@Nonnull
	@Override
	public FhirPatch newPatch(IPrimitiveType<Boolean> theIncludeMeta) {
		FhirPatch fhirPatch = super.newPatch(theIncludeMeta);
		fhirPatch.addIgnorePath("*.text.div");
		fhirPatch.addIgnorePath("*.id");

		return fhirPatch;
	}

	/**
	 * Sorting extensions to harmonize position by ordering through Similarity then Url
	 */
	private Comparator<IBaseExtension<?, ?>> extensionUrlSimilarityComparator(IDomainResource otherResource) {
		return (e1, e2) -> {
			boolean targetHas1 = otherResource.getExtension().stream().anyMatch(e -> e.getUrl().equals(e1.getUrl()));
			boolean targetHas2 = otherResource.getExtension().stream().anyMatch(e -> e.getUrl().equals(e2.getUrl()));
			if (targetHas1 == targetHas2) {
				return e1.getUrl().compareTo(e2.getUrl());
			} else if (targetHas1) {
				return 1;
			} else {
				return -1;
			}
		};
	}
}
