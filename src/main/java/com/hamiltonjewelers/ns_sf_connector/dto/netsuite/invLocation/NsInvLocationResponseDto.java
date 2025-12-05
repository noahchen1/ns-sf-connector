package com.hamiltonjewelers.ns_sf_connector.dto.netsuite.invLocation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class NsInvLocationResponseDto {
    private Integer count;
    private boolean hasMore;

    @JsonProperty("items")
    private List<InvLocation> invLocations;

    private int offset;
    private int totalResults;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public List<InvLocation> getInvLocations() {
        return invLocations;
    }

    public void setResults(List<InvLocation> invLocations) {
        this.invLocations = invLocations;
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

    public static class InvLocation {
        private Long item;
        private Integer location;

        @JsonProperty("onhandqty")
        private Integer quantityOnHand;

        public Long getItem() {
            return item;
        }

        public void setItem(Long item) {
            this.item = item;
        }

        public Integer getLocation() {
            return location;
        }

        public void setLocation(Integer location) {
            this.location = location;
        }

        public Integer getQuantityOnHand() {
            return quantityOnHand;
        }

        public void setQuantityOnHand(Integer quantityOnHand) {
            this.quantityOnHand = quantityOnHand;
        }

        @Override
        public String toString() {
            return String.format("NsInventoryLocation {Item=%s, Location=%s, QuantityOnHand=%s}",
                    item,
                    location,
                    quantityOnHand
            );
        }
    }
}

