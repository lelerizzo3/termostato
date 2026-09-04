package com.termostato.security;

import com.termostato.config.ConfigurationService;
import com.termostato.domain.model.SystemConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyAuthenticationFilterTest {

    private final ConfigurationService configuration = mock(ConfigurationService.class);
    private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(configuration);
    private final SystemConfiguration securedConfiguration = new SystemConfiguration(
            new BigDecimal("0.3"), false, null, 60, 3, 30,
            "https://ntfy.sh", "sliverd", false, "http://sensor", "http://relay", "./data/test.db",
            List.of("first-key", "second-key"));

    @Test
    void chiaveValidaProsegueLaRichiesta() throws Exception {
        when(configuration.current()).thenReturn(securedConfiguration);
        MockHttpServletRequest request = requestWithKey("second-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void chiaveAssenteRestituisceUnauthorized() throws Exception {
        when(configuration.current()).thenReturn(securedConfiguration);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/config");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("ApiKey", response.getHeader("WWW-Authenticate"));
        assertNull(chain.getRequest());
    }

    @Test
    void chiaveNonConfigurataRestituisceUnauthorized() throws Exception {
        when(configuration.current()).thenReturn(securedConfiguration);
        MockHttpServletRequest request = requestWithKey("wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    private static MockHttpServletRequest requestWithKey(String key) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/config");
        request.addHeader(ApiKeyAuthenticationFilter.HEADER_NAME, key);
        return request;
    }
}
