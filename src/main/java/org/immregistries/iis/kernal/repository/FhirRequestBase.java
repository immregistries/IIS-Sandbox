package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Bundle;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class FhirRequestBase {

	@Autowired
	RepositoryClientFactory repositoryClientFactory;

	public static final String GOLDEN_SYSTEM_TAG = "http://hapifhir.io/fhir/NamingSystem/mdm-record-status";
	public static final String GOLDEN_RECORD = "GOLDEN_RECORD";

	private static final TokenCriterion NOT_GOLDEN_CRITERION= new TokenCriterion("_tag:not",GOLDEN_SYSTEM_TAG,GOLDEN_RECORD);

	MethodOutcome save(IBaseResource resource, ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		MethodOutcome outcome;
		try {
			IUpdateTyped query = fhirClient.update().resource(resource);
			int size = where.length;
			query = query.conditional().where(NOT_GOLDEN_CRITERION);
			int i = 0;
			while (i < size) {
				query = ((IUpdateWithQuery) query).and(where[i]);
				i++;
			}
			outcome = query
				.execute();
		} catch (ResourceNotFoundException e){
			outcome = fhirClient.create().resource(resource)
				.execute();
		}
		return outcome;
	}
	public IBaseResource read(Class<? extends IBaseResource> aClass,String id) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		try {
			return fhirClient.read().resource(aClass).withId(id).execute();
		} catch (ResourceNotFoundException e){
			return null;
		}
	}
	Bundle searchGoldenRecord(Class<? extends IBaseResource> aClass,
									  ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		try {
			IQuery<Bundle> query = fhirClient.search().forResource(aClass).returnBundle(Bundle.class);
			int size = where.length;
			if (size > 0) {
				query = query.where(where[0]).withTag(GOLDEN_SYSTEM_TAG, GOLDEN_RECORD);
			}
			int i = 1;
			while (i < size) {
				query = query.and(where[i]);
				i++;
			}
			return  query.execute();
		} catch (ResourceNotFoundException e) {
			return  new Bundle();
		}
	}

	IBundleProvider searchGoldenRecord(IFhirResourceDao dao,
															 IQueryParameterType... where) {
		SearchParameterMap searchParameterMap = new SearchParameterMap();
		searchParameterMap.add("_tag", new TokenParam(GOLDEN_SYSTEM_TAG,GOLDEN_RECORD));
		try {
			int size = where.length;
			int i = 0;
			while (i < size) {
				searchParameterMap.add(where[i].getQueryParameterQualifier(),where[i])	;
				i++;
			}
			return dao.search(searchParameterMap, ServletHelper.requestDetailsWithPartitionName());

		} catch (ResourceNotFoundException e) {
			return null;
		}

	}
	Bundle searchRegularRecord(Class<? extends IBaseResource> aClass,
										ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		try {
			IQuery<Bundle> query = fhirClient.search().forResource(aClass).returnBundle(Bundle.class);
			int size = where.length;
			query = query.where(NOT_GOLDEN_CRITERION);
			int i = 0;
			while (i < size) {
				query = query.and(where[i]);
				i++;
			}
			return  query.execute();
		} catch (ResourceNotFoundException e) {
			return  new Bundle();
		}
	}
	Bundle search(
		Class<? extends IBaseResource> aClass,
		ICriterion... where) {
		IGenericClient fhirClient = ServletHelper.getFhirClient(repositoryClientFactory);
		try {
			IQuery<Bundle> query = fhirClient.search().forResource(aClass).returnBundle(Bundle.class);
			int size = where.length;
			if (size > 0) {
				query = query.where(where[0]);
			}
			int i = 1;
			while (i < size) {
				query = query.and(where[i]);
				i++;
			}
			return  query.execute();
		} catch (ResourceNotFoundException e) {
			return  new Bundle();
		}
	}
}
