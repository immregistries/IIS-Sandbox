package org.immregistries.iis.kernal.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.immregistries.iis.kernal.controllers.filters.RestTenantUrlFilter;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class RestTenantUrlFilterTest {

    @Test
    public void testDoFilterInternal_TenantRequest() throws ServletException, IOException {
        RestTenantUrlFilter filter = new RestTenantUrlFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/rest/tenant/123/patientMaster");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // We can't easily verify the log output without a more complex setup or a
        // custom appender,
        // but we can verify that the chain continued.
    }

    @Test
    public void testDoFilterInternal_NonTenantRequest() throws ServletException, IOException {
        RestTenantUrlFilter filter = new RestTenantUrlFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/other/path");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
