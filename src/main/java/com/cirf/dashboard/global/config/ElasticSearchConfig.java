package com.cirf.dashboard.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.time.Duration;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.cirf.dashboard.domain.analysis.repository")
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticSearchConfig extends ElasticsearchConfiguration {
    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.elasticsearch.uris}")
    private String esUri;

    @Override
    public ClientConfiguration clientConfiguration() {
        // Remove http:// or https:// from URI
        String host = esUri.replace("http://", "").replace("https://", "");

        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder =
                ClientConfiguration.builder().connectedTo(host);

        // Add authentication if password is provided
        if (password != null && !password.isEmpty()) {
            builder.withBasicAuth(username, password);
        }

        return builder
                .withConnectTimeout(Duration.ofSeconds(30))
                .withSocketTimeout(Duration.ofSeconds(30))
                .build();
    }
}
