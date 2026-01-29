/*-
 * #%L
 * HAPI FHIR - Master Data Management
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.immregistries.iis.fhir.mdm;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.mdm.api.MdmConstants;
import ca.uhn.fhir.mdm.interceptor.MdmSearchExpandingInterceptor;
import ca.uhn.fhir.mdm.svc.MdmSearchExpansionSvc;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.util.ICachedSearchDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Fix to be thoroughly tested to allow metadata operation when multitenant environement is active
 */
@Interceptor
public class MdmIisSearchExpandingInterceptor extends MdmSearchExpandingInterceptor {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final MdmSearchExpansionSvc.IParamTester PARAM_TESTER = (paramName, param) -> {
		boolean retVal = false;
		if (param instanceof ReferenceParam) {
			retVal = ((ReferenceParam) param).isMdmExpand();
		} else if (param instanceof TokenParam) {
			retVal = ((TokenParam) param).isMdmExpand();
		}
		return retVal;
	};

	@Autowired
	private JpaStorageSettings myStorageSettings;

	@Autowired
	private MdmSearchExpansionSvc myMdmSearchExpansionSvc;

	@Hook(
		value = Pointcut.STORAGE_PRESEARCH_REGISTERED,
		order = MdmConstants.ORDER_PRESEARCH_REGISTERED_MDM_SEARCH_EXPANDING_INTERCEPTOR)
	@Override
	public void hook(
		RequestDetails theRequestDetails,
		SearchParameterMap theSearchParameterMap,
		ICachedSearchDetails theSearchDetails) {

		if (myStorageSettings.isAllowMdmExpansion()) {
			logger.info("Search Details ResourceType {}", theSearchDetails.getResourceType());
			if (theSearchDetails.getResourceType() != null) {
				String resourceType = theSearchDetails.getResourceType();
				if ("StructureDefinition".equals(resourceType)) {
					return;
				}
				myMdmSearchExpansionSvc.expandSearchAndStoreInRequestDetails(
					resourceType, theRequestDetails, theSearchParameterMap, PARAM_TESTER);
			}
		}
	}
}
