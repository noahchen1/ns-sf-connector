package com.hamiltonjewelers.ns_sf_connector.utils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.UUID;

public class WorkerIdGenerator {
    private static final String ENV_VAR = "WORKER_ID";
    private static final int MAX_LEN = 100;

    private WorkerIdGenerator() {}

    public static String getWorkerId() {
        String env = System.getenv(ENV_VAR);
        if (env != null && !env.isBlank()) return sanitizeAndTrim(env);

        String host = "unkown-host";

        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            System.err.println("Could not get hostname: " + e);
        }

        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

        String id = String.format("%s@%s", host, pid, UUID.randomUUID());

        return sanitizeAndTrim(id);
    }

    private static String sanitizeAndTrim(String s) {
        String cleaned = s.replaceAll("[^A-Za-z0-9._\\-]", "-");

        return cleaned.length() <= MAX_LEN ? cleaned : cleaned.substring(0, MAX_LEN);
    }
}
