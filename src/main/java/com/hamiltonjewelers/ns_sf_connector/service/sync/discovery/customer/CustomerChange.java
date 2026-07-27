package com.hamiltonjewelers.ns_sf_connector.service.sync.discovery.customer;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;

public record CustomerChange(
        int netsuiteId,
        CustomerItemDto netsuiteCustomer,
        AccountDto.AccountRecord salesforceAccount
) {
}
