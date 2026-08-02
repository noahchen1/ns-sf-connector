package com.hamiltonjewelers.ns_sf_connector.service.sync.discovery.customer;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.dto.CustomerChange;
import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
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
        CustomerDto netsuite = netsuiteCustomer(42, now.minusMinutes(5));
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

    private CustomerDto netsuiteCustomer(int id, LocalDateTime modifiedAt) {
        return new CustomerDto(
                id, null, modifiedAt, null, null, null, null, null, 0, 0, List.of()
        );
    }

    private AccountDto.AccountRecord salesforceAccount(int netsuiteId, LocalDateTime modifiedAt) {
        return new AccountDto.AccountRecord(
                null, "001-test", null, netsuiteId, null, null, null, modifiedAt
        );
    }
}
