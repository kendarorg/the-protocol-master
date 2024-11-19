package org.kendar.http.utils.ssl;

import java.util.Objects;

public class SSLDomain {
    private String id;
    private String address;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SSLDomain sslDomain = (SSLDomain) o;
        return address.equals(sslDomain.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}
