package com.termostato.external;

import com.termostato.config.BootstrapProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Component
public class RestClientFactory {

    private final BootstrapProperties properties;

    public RestClientFactory(BootstrapProperties properties) {
        this.properties = properties;
    }

    public <T> T createProxy(String baseUrl, Class<T> contract) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = properties.getHttpTimeoutMillis() > 0 ? properties.getHttpTimeoutMillis() : 3000;
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMillis));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMillis));
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(contract);
    }
}
