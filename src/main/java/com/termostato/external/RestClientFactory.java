package com.termostato.external;

import com.termostato.config.BootstrapProperties;
import com.termostato.config.ConfigurationService;
import com.termostato.mock.MockDeviceProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Component
public class RestClientFactory {

    private final BootstrapProperties properties;
    private final ConfigurationService configuration;
    private final MockDeviceProperties mockDeviceProperties;

    public RestClientFactory(BootstrapProperties properties,
                             ConfigurationService configuration,
                             MockDeviceProperties mockDeviceProperties) {
        this.properties = properties;
        this.configuration = configuration;
        this.mockDeviceProperties = mockDeviceProperties;
    }

    public <T> T createProxy(String baseUrl, Class<T> contract) {
        return createProxy(baseUrl, contract, false);
    }

    public <T> T createProxy(String baseUrl, Class<T> contract, boolean authenticateMockDevice) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = properties.getHttpTimeoutMillis() > 0 ? properties.getHttpTimeoutMillis() : 3000;
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMillis));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMillis));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory);
        if (authenticateMockDevice && mockDeviceProperties.isEnabled()
                && !configuration.current().apiKeys().isEmpty()) {
            builder.defaultHeader("X-API-Key", configuration.current().apiKeys().getFirst());
        }

        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(contract);
    }
}
