package com.hamiltonjewelers.ns_sf_connector.service.sync.customer.operation;

import com.hamiltonjewelers.ns_sf_connector.client.ns.auth.NsAuthClient;
import com.hamiltonjewelers.ns_sf_connector.client.ns.customer.NsCustomerClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.account.SfAccountClient;
import com.hamiltonjewelers.ns_sf_connector.client.sf.auth.SfAuthClient;
import com.hamiltonjewelers.ns_sf_connector.dto.CustomerState;
import com.hamiltonjewelers.ns_sf_connector.dto.CustomerSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.CustomerMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateNetsuiteCustomerTests {
    private static final String SALESFORCE_ID = "001000000000001AAA";

    @Mock private CustomerMapping mapping;
    @Mock private NsAuthClient nsAuthClient;
    @Mock private NsCustomerClient nsCustomerClient;
    @Mock private SfAuthClient sfAuthClient;
    @Mock private SfAccountClient sfAccountClient;

    private CreateNetsuiteCustomer operation;

    @BeforeEach
    void setUp() {
        operation = new CreateNetsuiteCustomer(
                mapping,
                nsAuthClient,
                nsCustomerClient,
                sfAuthClient,
                sfAccountClient
        );
    }

    @Test
    void createsCustomerAndLinksSalesforceAccount() {
        AccountDto.AccountRecord account = account(null);
        Map<String, Object> createFields = Map.of("externalId", SALESFORCE_ID);
        when(nsAuthClient.fetchAccessToken()).thenReturn("ns-token");
        when(nsCustomerClient.getCustomersBySalesforceId("ns-token", SALESFORCE_ID))
                .thenReturn(List.of());
        when(mapping.netsuiteCreateFields(account)).thenReturn(createFields);
        when(nsCustomerClient.createCustomer("ns-token", createFields)).thenReturn(42);
        when(sfAuthClient.fetchAccessToken()).thenReturn("sf-token");

        operation.execute(job(), context(account));

        verify(nsCustomerClient).createCustomer("ns-token", createFields);
        verify(sfAccountClient).updateAccount(
                "sf-token",
                SALESFORCE_ID,
                Map.of("Netsuite_Id__c", 42)
        );
    }

    @Test
    void reusesCustomerCreatedByAnEarlierAttempt() {
        AccountDto.AccountRecord account = account(null);
        CustomerDto existing = new CustomerDto(
                42, null, null, null, null, null, null, SALESFORCE_ID, 0, 0, List.of()
        );
        when(nsAuthClient.fetchAccessToken()).thenReturn("ns-token");
        when(nsCustomerClient.getCustomersBySalesforceId("ns-token", SALESFORCE_ID))
                .thenReturn(List.of(existing));
        when(sfAuthClient.fetchAccessToken()).thenReturn("sf-token");

        operation.execute(job(), context(account));

        verify(nsCustomerClient, never()).createCustomer(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap()
        );
        verify(sfAccountClient).updateAccount(
                "sf-token",
                SALESFORCE_ID,
                Map.of("Netsuite_Id__c", 42)
        );
    }

    @Test
    void doesNothingWhenAccountWasAlreadyLinked() {
        operation.execute(job(), context(account(42)));

        verifyNoInteractions(
                mapping,
                nsAuthClient,
                nsCustomerClient,
                sfAuthClient,
                sfAccountClient
        );
    }

    private SyncJob job() {
        SyncJob job = new SyncJob();
        job.setSourceRecordId(SALESFORCE_ID);
        return job;
    }

    private CustomerSyncContext context(AccountDto.AccountRecord account) {
        return new CustomerSyncContext(null, new CustomerState(null, account));
    }

    private AccountDto.AccountRecord account(Integer netsuiteId) {
        return new AccountDto.AccountRecord(
                null,
                SALESFORCE_ID,
                "Ada Lovelace",
                netsuiteId,
                "Ada",
                "Lovelace",
                "ada@example.com",
                null
        );
    }
}
