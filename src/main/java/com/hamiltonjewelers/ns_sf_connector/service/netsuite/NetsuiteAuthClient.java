package com.hamiltonjewelers.ns_sf_connector.service.netsuite;

import com.hamiltonjewelers.ns_sf_connector.config.NsConfig;
import com.hamiltonjewelers.ns_sf_connector.service.jwt.JwtService;
import com.hamiltonjewelers.ns_sf_connector.service.jwt.NsKeyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.interfaces.ECPrivateKey;

@Service
public class NetsuiteAuthClient {
    private final NsConfig config;
    private final JwtService jwtService;

    @Autowired
    public NetsuiteAuthClient(NsConfig config, JwtService jwtService) {
        this.config = config;
        this.jwtService = jwtService;
    }

    public String fetchAccessToken() throws Exception {
        ECPrivateKey privateKey = NsKeyLoader.loadEcPrivateKey("ns.pem");

        String jwt = jwtService.createJwt(
                config.getClientId(),
                config.getCertificateId(),
                config.getTokenUrl(),
                config.getScope(),
                privateKey);

        return jwt;
    }
}
