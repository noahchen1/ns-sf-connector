package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;

public record CustomerState(CustomerDto netsuiteCustomer, AccountDto.AccountRecord salesforceAccount) {
}
