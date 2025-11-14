package com.hamiltonjewelers.ns_sf_connector.dto.netsuite.customer;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerDto {
    @JsonProperty("internalid")
    private int internalId;

    @JsonProperty("custid")
    private String custId;

    private String email;
    private String firstname;
    private String lastname;
    private String address;
    private int subsidiary;
    private int rowId;

    public Integer getInternalId() {
        return internalId;
    }

    public String getCustId() {
        return custId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public int getSubsidiary() {
        return subsidiary;
    }

    public String getAddress() {
        return address;
    }

    public int getId() {
        return rowId;
    }

    public void setInternalId(int internal_id) {
        this.internalId = internal_id;
    }

    public void setCustId(String custId) {
        this.custId = custId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setSubsidiary(int subsidiary) {
        this.subsidiary = subsidiary;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setId(int rowId) {
        this.rowId = rowId;
    }

    @Override
    public String toString() {
        return "CustomerDTO{" +
                "Internal_Id=" + internalId +
                ", Cust_ID=" + custId +
                ", email=" + email +
                ", firstname=" + firstname +
                ", lastname=" + lastname +
                ", subsidiary=" + subsidiary +
                ", rowId=" + rowId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CustomerDto that = (CustomerDto) o;

        return internalId == that.internalId &&
                subsidiary == that.subsidiary &&
                Objects.equals(custId, that.custId) &&
                Objects.equals(email, that.email) &&
                Objects.equals(firstname, that.firstname) &&
                Objects.equals(lastname, that.lastname) &&
                Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalId, custId, email, firstname, lastname, address, subsidiary);
    }
}
