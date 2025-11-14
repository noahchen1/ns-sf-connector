package com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer;

import java.util.List;

public class CustomerResDto {
    private List<LinkDto> links;
    private int count;
    private boolean hasMore;
    private List<CustomerItemDto> items;
    private int offset;
    private int totalResults;

    public List<LinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<LinkDto> links) {
        this.links = links;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public List<CustomerItemDto> getItems() {
        return items;
    }

    public void setItems(List<CustomerItemDto> items) {
        this.items = items;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    @Override
    public String toString() {
        return "CustomerResponseDto{" +
                "links=" + links +
                ", count=" + count +
                ", hasMore=" + hasMore +
                ", items=" + items +
                ", offset=" + offset +
                ", totalResults=" + totalResults +
                '}';
    }
}