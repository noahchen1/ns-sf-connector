package com.hamiltonjewelers.ns_sf_connector.config;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.JwtService;
import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsKeyLoader;
import com.hamiltonjewelers.ns_sf_connector.utils.JoinUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.security.interfaces.ECPrivateKey;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "netsuite")
public class NsConfig {
    private String clientId;
    private String certId;
    private String grantType;
    private List<String> scope;
    private String clientAssertionType;
    private String baseUrl;

    public String getBaseUrl() { return baseUrl; }

    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCertId() {
        return certId;
    }

    public void setCertId(String certId) {
        this.certId = certId;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getClientAssertionType() {
        return clientAssertionType;
    }

    public void setClientAssertionType(String clientAssertionType) {
        this.clientAssertionType = clientAssertionType;
    }
}
