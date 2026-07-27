package com.hamiltonjewelers.ns_sf_connector.service.sync.customer.conflict;

import com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer.CustomerDto;
import com.hamiltonjewelers.ns_sf_connector.dto.sf.account.AccountDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CustomerConflictResolver {
    private final CustomerPatchPlanner patchPlanner;

    public CustomerConflictResolver(CustomerPatchPlanner patchPlanner) {
        this.patchPlanner = patchPlanner;
    }

    public CustomerPatches resolve(
            CustomerDto netsuite,
            AccountDto.AccountRecord salesforce
    ) {
        return patchPlanner.plan(
                netsuite,
                salesforce,
                netsuiteWins(netsuite.getLastModifiedDate(), salesforce.getLastModifiedDate())
        );
    }

    private boolean netsuiteWins(LocalDateTime netsuiteModified, LocalDateTime salesforceModified) {
        return salesforceModified == null
                || (netsuiteModified != null && !salesforceModified.isAfter(netsuiteModified));
    }
}
