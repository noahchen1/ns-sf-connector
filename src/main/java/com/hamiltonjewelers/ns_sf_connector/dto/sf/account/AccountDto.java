package com.hamiltonjewelers.ns_sf_connector.dto.sf.account;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AccountDto {
    private int totalSize;
    private boolean done;
    List<AccountRecord> records;

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public List<AccountRecord> getRecords() {
        return records;
    }

    public void setRecords(List<AccountRecord> records) {
        this.records = records;
    }

    public static class AccountRecord {
        private Attributes attributes;

        @JsonProperty("Id")
        private String Id;

        @JsonProperty("Netsuite_Id__c")
        private Integer netsuiteId;

        @JsonProperty("First_Name__c")
        private String firstName;

        @JsonProperty("Last_Name__c")
        private String lastName;

        @JsonProperty("Account_Email__c")
        private String email;

        public Attributes getAttributes() {
            return attributes;
        }

        public void setAttributes(Attributes attributes) {
            this.attributes = attributes;
        }

        public String getId() {
            return Id;
        }

        public void setId(String Id) {
            this.Id = Id;
        }

        public Integer getNetsuiteId() { return netsuiteId; }

        public void setNetsuiteId(Integer netsuiteId) { this.netsuiteId = netsuiteId; }

        public String getFirstName() { return firstName; }

        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }

        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }

        public void setEmail(String email) { this.email = email; }

        @Override
        public String toString() {
            return String.format("AccountRecord {Id='%s', First_Name='%s', Last_Name='%s', Email='%s', attributes=%s",
                    Id,
                    firstName,
                    lastName,
                    email,
                    attributes
            );
        }
    }

    public static class Attributes {
        private String type;
        private String url;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}



