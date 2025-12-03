package org.immregistries.iis.kernal.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class TenantRequestLoggingFilterTest {

    @Test
    public void testDoFilterInternal_TenantRequest() throws ServletException, IOException {
        TenantRequestLoggingFilter filter = new TenantRequestLoggingFilter();
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
        TenantRequestLoggingFilter filter = new TenantRequestLoggingFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/other/path");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
