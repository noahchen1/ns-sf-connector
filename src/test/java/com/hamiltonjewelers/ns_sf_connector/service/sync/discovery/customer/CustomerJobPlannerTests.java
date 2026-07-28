package com.hamiltonjewelers.ns_sf_connector.service.sync.discovery.customer;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerItemDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.service.sync.enums.SyncSystem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerJobPlannerTests {
    private final CustomerJobPlanner planner = new CustomerJobPlanner();
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 26, 12, 0);

    @Test
    void createsSalesforceAccountForNetsuiteOnlyCustomer() {
        List<SyncJob> jobs = planner.plan(
                List.of(new CustomerChange(42, netsuiteCustomer(42, now), null)),
                now
        );

        assertThat(jobs).singleElement().satisfies(job -> {
            assertThat(SyncRoute.from(job).is(
                    SyncSystem.NETSUITE,
                    SyncSystem.SALESFORCE,
                    SyncOperation.INSERT
            )).isTrue();
            assertThat(job.getSourceRecordId()).isEqualTo("42");
        });
    }

    @Test
    void updatesTheOlderSystem() {
        CustomerItemDto netsuite = netsuiteCustomer(42, now.minusMinutes(5));
        AccountDto.AccountRecord salesforce = salesforceAccount(42, now);

        List<SyncJob> jobs = planner.plan(
                List.of(new CustomerChange(42, netsuite, salesforce)),
                now
        );

        assertThat(jobs).singleElement().satisfies(job -> assertThat(SyncRoute.from(job).is(
                SyncSystem.SALESFORCE,
                SyncSystem.NETSUITE,
                SyncOperation.UPDATE
        )).isTrue());
    }

    @Test
    void ignoresSalesforceOnlyRecordUntilNetsuiteCreationIsSupported() {
        List<SyncJob> jobs = planner.plan(
                List.of(new CustomerChange(42, null, salesforceAccount(42, now))),
                now
        );

        assertThat(jobs).isEmpty();
    }

    private CustomerItemDto netsuiteCustomer(int id, LocalDateTime modifiedAt) {
        CustomerItemDto customer = new CustomerItemDto();
        customer.setInternalId(id);
        customer.setLastModifiedDate(modifiedAt);
        return customer;
    }

    private AccountDto.AccountRecord salesforceAccount(int netsuiteId, LocalDateTime modifiedAt) {
        AccountDto.AccountRecord account = new AccountDto.AccountRecord();
        account.setId("001-test");
        account.setNetsuiteId(netsuiteId);
        account.setLastModifiedDate(modifiedAt);
        return account;
    }
}
