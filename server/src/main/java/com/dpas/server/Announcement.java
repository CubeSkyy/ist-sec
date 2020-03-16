package com.dpas.server;

import java.util.*;
import java.text.SimpleDateFormat;

public class Announcement {

    private String key;
    public String date;
    public String message;
    public List<Announcement> referedAnnouncements;

    public Announcement(String key, String message){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd@HH-mm-ss");
        this.key = key;
        this.date = sdf.format(new Date());
        this.message = message;
        this.referedAnnouncements = new ArrayList<Announcement>();
    }

    public String getKey() {
        return this.key;
    }

    public String getDate() {
        return this.date;
    }

    public String getMessage() {
        return this.message;
    }

    public void addAnnouncement(Announcement a){
        this.referedAnnouncements.add(a);
    }

    @java.lang.Override
    public String toString() {
        return "- Announcement{ key  = '" + key + "date = '" + date + '\'' + " } -----------\n" +
                "   Message:'" + message + '\'' + "\n" +
                "   Refered Announcements: \n   - " + referedAnnouncements + "\n" +
                "-----------------------------------------------------------";
    }
}