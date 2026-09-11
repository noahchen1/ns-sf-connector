package com.hamiltonjewelers.ns_sf_connector.dto;

import java.util.Map;

public record ItemPatches(
        Map<String, Object> salesforcePatch,
        Map<String, Object> netsuitePatch
) {
}
