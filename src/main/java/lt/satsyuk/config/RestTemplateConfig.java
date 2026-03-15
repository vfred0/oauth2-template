package lt.satsyuk.config;

import io.micrometer.observation.ObservationRegistry;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(ObservationRegistry observationRegistry, KeycloakProperties keycloakProperties) {
        Duration connectTimeout = keycloakProperties.getConnectTimeout();
        Duration readTimeout = keycloakProperties.getReadTimeout();

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.of(readTimeout))
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(connectTimeout))
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom()
                        .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                                .setDefaultConnectionConfig(connectionConfig)
                                .build())
                        .setDefaultRequestConfig(requestConfig)
                        .disableAutomaticRetries()
                        .build()
        );
        requestFactory.setReadTimeout(readTimeout);

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setObservationRegistry(observationRegistry);
        return restTemplate;
    }
}