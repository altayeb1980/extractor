package org.scrawls.extractor;

import javafx.beans.property.SimpleStringProperty;

public class SummaryData {


    private final SimpleStringProperty timeRunning;
    private final SimpleStringProperty totalPhoneNumber;


    SummaryData(String timeRunning, String totalPhoneNumber) {
        this.timeRunning = new SimpleStringProperty(timeRunning);
        this.totalPhoneNumber = new SimpleStringProperty(totalPhoneNumber);

    }

    public final SimpleStringProperty timeRunningProperty() {
        return this.timeRunning;
    }

    public final SimpleStringProperty totalPhoneNumberProperty() {
        return this.totalPhoneNumber;
    }

    public String getTimeRunning() {
        return timeRunning.get();
    }

    public void setTimeRunning(String time) {
        timeRunningProperty().set(time);
    }

    public String getPhoneNumber() {
        return totalPhoneNumber.get();
    }

    public void setPhoneNumber(String phone) {
        totalPhoneNumberProperty().set(phone);
    }
}
