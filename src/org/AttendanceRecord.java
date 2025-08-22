package org;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AttendanceRecord {
    private final StringProperty srno;
    private final StringProperty date;
    private final StringProperty weekday;
    private final StringProperty checkin;
    private final StringProperty checkout;
    private final StringProperty activetime;
    private final StringProperty overtime;
    private final StringProperty status;
    private final StringProperty attendance;


    public AttendanceRecord(String srno, String date, String weekday, String checkin, String checkout, String activetime, String overtime, String status , String attendace) {
        this.srno = new SimpleStringProperty(srno);
        this.date = new SimpleStringProperty(date);
        this.weekday = new SimpleStringProperty(weekday);
        this.checkin = new SimpleStringProperty(checkin);
        this.checkout = new SimpleStringProperty(checkout);
        this.activetime = new SimpleStringProperty(activetime);
        this.overtime = new SimpleStringProperty(overtime);
        this.status = new SimpleStringProperty(status);
        this.attendance = new SimpleStringProperty(attendace);
    }

    public StringProperty srnoProperty() {
        return srno;
    }

    public StringProperty dateProperty() {
        return date;
    }

    public StringProperty weekdayProperty() {
        return weekday;
    }

    public StringProperty checkinProperty() {
        return checkin;
    }

    public StringProperty checkoutProperty() {
        return checkout;
    }

    public StringProperty activetimeProperty() {
        return activetime;
    }

    public StringProperty overtimeProperty() {
        return overtime;
    }

    public StringProperty statusProperty() {
        return status;
    }
    public StringProperty attendaceProperty() {
        return attendance;
    }
}
