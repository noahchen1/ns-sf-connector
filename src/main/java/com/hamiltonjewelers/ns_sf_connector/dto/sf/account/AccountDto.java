package com.hamiltonjewelers.ns_sf_connector.dto.sf.account;

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
        private String Id;
        private String Name;

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

        public String getName() {
            return Name;
        }

        public void setName(String Name) {
            this.Name = Name;
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



