package com.example.handshake;

import android.net.Uri;

import java.security.PrivateKey;
import java.util.Date;

public class CreateGroup {

    private String groupDpImageUri;
    private String groupName;
    private String groupDescription;
    private String groupCreaterPhoneNumber;
    private Double groupLongitude;
    private Double groupLatitude;
    private String groupRange;
    private String groupId;

    private String groupLastMassage;
    private String groupLastMassageSenderName;
    private Date groupLastMassageDate;

    public CreateGroup() {
    }

    public CreateGroup(String groupDpImageUri, String groupName, String groupDescription, String groupRange) {
        this.groupDpImageUri = groupDpImageUri;
        this.groupName = groupName;
        this.groupDescription = groupDescription;
        this.groupRange = groupRange;
    }

    public CreateGroup(String groupLastMassage, String groupLastMassageSenderName, Date groupLastMassageDate) {
        this.groupLastMassage = groupLastMassage;
        this.groupLastMassageSenderName = groupLastMassageSenderName;
        this.groupLastMassageDate = groupLastMassageDate;
    }

    public CreateGroup(String groupDpImageUri, String groupName, String groupDescription, String groupCreaterPhoneNumber, Double groupLongitude, Double groupLatitude, String groupId) {
        this.groupDpImageUri = groupDpImageUri;
        this.groupName = groupName;
        this.groupDescription = groupDescription;
        this.groupCreaterPhoneNumber = groupCreaterPhoneNumber;
        this.groupLongitude = groupLongitude;
        this.groupLatitude = groupLatitude;
        this.groupId = groupId;
    }

    public String getGroupRange() {
        return groupRange;
    }

    public void setGroupRange(String groupRange) {
        this.groupRange = groupRange;
    }

    public String getGroupDpImageUri() {
        return groupDpImageUri;
    }

    public void setGroupDpImageUri(String groupDpImageUri) {
        this.groupDpImageUri = groupDpImageUri;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }

    public String getGroupCreaterPhoneNumber() {
        return groupCreaterPhoneNumber;
    }

    public void setGroupCreaterPhoneNumber(String groupCreaterPhoneNumber) {
        this.groupCreaterPhoneNumber = groupCreaterPhoneNumber;
    }

    public Double getGroupLongitude() {
        return groupLongitude;
    }

    public void setGroupLongitude(Double groupLongitude) {
        this.groupLongitude = groupLongitude;
    }

    public Double getGroupLatitude() {
        return groupLatitude;
    }

    public void setGroupLatitude(Double groupLatitude) {
        this.groupLatitude = groupLatitude;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupLastMassage() {
        return groupLastMassage;
    }

    public void setGroupLastMassage(String groupLastMassage) {
        this.groupLastMassage = groupLastMassage;
    }

    public String getGroupLastMassageSenderName() {
        return groupLastMassageSenderName;
    }

    public void setGroupLastMassageSenderName(String groupLastMassageSenderName) {
        this.groupLastMassageSenderName = groupLastMassageSenderName;
    }

    public Date getGroupLastMassageDate() {
        return groupLastMassageDate;
    }

    public void setGroupLastMassageDate(Date groupLastMassageDate) {
        this.groupLastMassageDate = groupLastMassageDate;
    }
}
