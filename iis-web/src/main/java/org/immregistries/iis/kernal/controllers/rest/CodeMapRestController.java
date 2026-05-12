package org.immregistries.iis.kernal.controllers.rest;

import org.immregistries.codebase.client.CodeMap;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.IisRequestAttribute;
import org.immregistries.iis.kernal.controllers.IisRestPath;
import org.immregistries.iis.kernal.persisted.entities.Tenant;
import org.immregistries.iis.kernal.services.CodeMapManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping({ IisRestPath.REST_TENANT_PATH + IisRestPath.BasePath.CODE_MAPS_PATH,
        IisRestPath.BasePath.REST_PATH + IisRestPath.BasePath.CODE_MAPS_PATH})
public class CodeMapRestController {

    @Autowired
    private CodeMapManagerService codeMapManagerService;

    @GetMapping
    public CodeMap getCodeMaps(
            @RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant) {
        return codeMapManagerService.getCodeMap();
    }

    @GetMapping("/search")
    public Collection<?> getCodesForTable(
            @RequestParam(name = "tableName") String tableName,
            @RequestAttribute(IisRequestAttribute.TENANT_REQUEST_ATTRIBUTE) Tenant tenant) {
        CodesetType codesetType = CodesetType.valueOf(tableName);
        return codeMapManagerService.getCodeMap().getCodesForTable(codesetType);
    }
}
