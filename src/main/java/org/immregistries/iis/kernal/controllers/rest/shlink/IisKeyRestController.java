package org.immregistries.iis.kernal.controllers.rest.shlink;

import com.nimbusds.jose.jwk.JWK;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.fhir.security.UserAccessUtil;
import org.immregistries.iis.kernal.logic.KeyStoreService;
import org.immregistries.iis.kernal.persisted.model.IisKey;
import org.immregistries.iis.kernal.persisted.model.UserAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping()
public class IisKeyRestController {

    @Autowired
    KeyStoreService keyStoreService;

    @GetMapping("/.well-known/jwks.json")
    public List<JWK> doGetWellKnown(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        UserAccess userAccess = UserAccessUtil.get().getUserAccess();
        List<IisKey> iisKeys = keyStoreService.getKeys(userAccess);
        return iisKeys.stream().map(iisKey -> iisKey.jwk().toPublicJWK()).collect(Collectors.toList());
    }
}
