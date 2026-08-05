package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.config.NsConfig;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMappingTests {
    @Test
    void mapsSalesforcePersonAccountForNetsuiteCreation() {
        NsConfig config = new NsConfig();
        config.setDefaultCustomerSubsidiaryId("7");
        CustomerMapping mapping = new CustomerMapping(config);
        AccountDto.AccountRecord account = new AccountDto.AccountRecord(
                null,
                "001000000000001AAA",
                "Ada Lovelace",
                null,
                "Ada",
                "Lovelace",
                "ada@example.com",
                null
        );

        Map<String, Object> fields = mapping.netsuiteCreateFields(account);

        assertThat(fields)
                .containsEntry("externalId", account.id())
                .containsEntry("custentity_sfid", account.id())
                .containsEntry("isPerson", true)
                .containsEntry("firstName", "Ada")
                .containsEntry("lastName", "Lovelace")
                .containsEntry("email", "ada@example.com")
                .containsEntry("subsidiary", Map.of("id", "7"));
    }
}
