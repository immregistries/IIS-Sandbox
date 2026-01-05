package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.controllers.filters.RestTenantUrlFilter;
import org.immregistries.iis.kernal.logic.CodeMapManagerService;
import org.immregistries.iis.kernal.persisted.model.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping({RestUrlUtil.REST_TENANT_PATH + CodeMapRestController.CODEMAPS_PATH_SUFFIX, RestUrlUtil.REST + CodeMapRestController.CODEMAPS_PATH_SUFFIX})
public class CodeMapRestController {

	public static final String CODEMAPS_PATH_KEY = "codemaps";
	public static final String CODEMAPS_PATH_SUFFIX = "/" + CODEMAPS_PATH_KEY;
	@Autowired
	CodeMapManagerService codeMapManagerService;

	@GetMapping
    public CodeMap getCodeMaps(
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant) {
        return codeMapManagerService.getCodeMap();
    }

    @GetMapping("/search")
    public Collection<?> getCodesForTable(
            @RequestParam(name = "tableName") String tableName,
            @RequestAttribute(RestTenantUrlFilter.TENANT_REQUEST_ATTRIBUTE) Tenant tenant) {
        CodesetType codesetType = CodesetType.valueOf(tableName);
        return codeMapManagerService.getCodeMap().getCodesForTable(codesetType);
    }
}
