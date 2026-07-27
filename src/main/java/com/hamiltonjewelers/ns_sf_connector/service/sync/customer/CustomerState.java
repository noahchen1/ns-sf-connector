package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;

public record CustomerState(
        CustomerDto netsuiteCustomer,
        AccountDto.AccountRecord salesforceAccount
) {
    public CustomerDto requireNetsuiteCustomer(int netsuiteId) {
        if (netsuiteCustomer == null) {
            throw new IllegalStateException("NetSuite Customer is missing for ID " + netsuiteId);
        }
        return netsuiteCustomer;
    }

    public AccountDto.AccountRecord requireSalesforceAccount(int netsuiteId) {
        if (salesforceAccount == null) {
            throw new IllegalStateException("Salesforce Account is missing for NetSuite ID " + netsuiteId);
        }
        return salesforceAccount;
    }
}
