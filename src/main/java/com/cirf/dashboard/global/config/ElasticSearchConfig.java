package com.cirf.dashboard.global.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
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

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // Parse URI
        String host = esUri.replace("http://", "").replace("https://", "");
        String[] hostParts = host.split(":");
        String hostname = hostParts[0];
        int port = hostParts.length > 1 ? Integer.parseInt(hostParts[1]) : 9200;

        // Create RestClient
        RestClient restClient;
        if (password != null && !password.isEmpty()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password)
            );

            restClient = RestClient.builder(new HttpHost(hostname, port, "http"))
                    .setHttpClientConfigCallback(httpClientBuilder ->
                            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                    .build();
        } else {
            restClient = RestClient.builder(new HttpHost(hostname, port, "http")).build();
        }

        // Create transport and client
        ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }
}

