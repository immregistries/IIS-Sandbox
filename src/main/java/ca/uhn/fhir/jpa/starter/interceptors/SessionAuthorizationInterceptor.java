package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import org.springframework.stereotype.Component;

import javax.interceptor.Interceptor;

@Component
@Interceptor
public class SessionAuthorizationInterceptor extends AuthorizationInterceptor {

}
