package com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer;

import java.util.List;

public class CustomerItemDto extends CustomerDto {
    private List<LinkDto> links;


    public List<LinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<LinkDto> links) {
        this.links = links;
    }

    @Override
    public String toString() {
        return "CustomerItem{internalId='" + getInternalId() +
                "', cust_id='" + getCustId() +
                "', email='" + getEmail() +
                "', firstname='" + getFirstname() +
                "', lastname='" + getLastname() +
                "', subsidiary='" + getSubsidiary() +
                "', address='" + getAddress() +
                "'}";
    }
}
