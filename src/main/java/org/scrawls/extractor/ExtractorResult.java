package org.scrawls.extractor;

import javafx.beans.property.SimpleStringProperty;

import java.util.List;
import java.util.Objects;

public class ExtractorResult {

    private SimpleStringProperty url;
    private SimpleStringProperty phoneNumber;

    public ExtractorResult(String url, String phoneNumber) {
        this.url = new SimpleStringProperty(url);
        this.phoneNumber = new SimpleStringProperty(phoneNumber);
    }

    public SimpleStringProperty urlProperty() {
        if (url == null) {
            url = new SimpleStringProperty(this, "url");
        }
        return url;
    }

    public SimpleStringProperty phoneNumberProperty() {
        if (phoneNumber == null) {
            phoneNumber = new SimpleStringProperty(this, "phoneNumber");
        }
        return phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber.get();
    }

    public void setPhoneNumber(String phone) {
        phoneNumber.set(phone);
    }

    public String getUrl() {
        return url.get();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtractorResult that = (ExtractorResult) o;
        return Objects.equals(getPhoneNumber(), that.getPhoneNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPhoneNumber());
    }

    @Override
    public String toString() {
        return "ExtractorResult{" +
                "url=" + getUrl() +
                ", phoneNumber=" + getPhoneNumber() +
                '}';
    }
}
