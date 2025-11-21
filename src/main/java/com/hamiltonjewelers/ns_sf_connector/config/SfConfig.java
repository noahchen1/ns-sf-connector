package com.hamiltonjewelers.ns_sf_connector.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "salesforce")
public class SfConfig {
    private String clientId;
    private String clientSecret;
    private String grantType;
    private String baseUrl;

    public String getBaseUrl() { return baseUrl; }

    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getClientId() { return clientId; }

    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }

    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getGrantType() { return grantType; }

    public void setGrantType(String grantType) { this.grantType = grantType; }
}
