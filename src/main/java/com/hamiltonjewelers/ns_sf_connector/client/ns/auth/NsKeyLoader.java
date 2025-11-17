package com.hamiltonjewelers.ns_sf_connector.client.ns.auth;

import com.hamiltonjewelers.ns_sf_connector.NsSfConnectorApplication;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class NsKeyLoader {
    public static ECPrivateKey loadEcPrivateKey(String resourcePath) throws Exception {
        String keyPem;

        try (InputStream is = NsSfConnectorApplication.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("ns.pem is not found in resources");
            }

            keyPem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s", "");
        }

        byte[] keyBytes = Base64.getDecoder().decode(keyPem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PrivateKey privateKey = kf.generatePrivate(spec);

        return (ECPrivateKey) privateKey;
    }
}
