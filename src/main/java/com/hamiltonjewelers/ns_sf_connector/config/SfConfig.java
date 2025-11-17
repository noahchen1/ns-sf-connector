package com.hamiltonjewelers.ns_sf_connector.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "salesforce")
public class SfConfig {
    private String authUrl;
    private String accountUrl;
    private String clientId;
    private String clientSecret;
    private String grantType;

    public String getAuthUrl() { return authUrl; }

    public void setAuthUrl(String authUrl) { this.authUrl = authUrl; }

    public String getAccountUrl() { return accountUrl; }

    public void setAccountUrl(String accountUrl) { this.accountUrl = accountUrl; }

    public String getClientId() { return clientId; }

    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }

    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getGrantType() { return grantType; }

    public void setGrantType(String grantType) { this.grantType = grantType; }
}
