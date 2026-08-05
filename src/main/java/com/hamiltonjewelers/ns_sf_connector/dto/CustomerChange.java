package com.hamiltonjewelers.ns_sf_connector.dto;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;

public record CustomerChange(
        Integer netsuiteId,
        String salesforceId,
        CustomerDto netsuiteCustomer,
        AccountDto.AccountRecord salesforceAccount
) {
}
