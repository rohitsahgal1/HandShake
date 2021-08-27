package com.example.handshake;

import java.util.Date;
import java.util.HashMap;

public class Massage {

    private String massages,phoneNumber,images,chatId;
    private Date timeStamp;

    public Massage() {
    }

    public Massage(String massages, String phoneNumber, Date timeStamp) {
        this.massages = massages;
        this.phoneNumber = phoneNumber;
        this.timeStamp = timeStamp;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getMassages() {
        return massages;
    }

    public void setMassages(String massages) {
        this.massages = massages;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
}
