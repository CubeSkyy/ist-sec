package com.dpas.server;

import java.util.*;
import java.text.SimpleDateFormat;

public class AnnouncementBoard {
    public final String key;
    public List<Announcement> announcements;

    public AnnouncementBoard(String key){
        this.key = key;
        this.announcements = new ArrayList<Announcement>();
    }

    public String getKey(){
        return this.key;
    }

    public List<Announcement> getAnnouncements(){
        return this.announcements;
    }

    public void addAnnouncement(Announcement a){
        this.announcements.add(a);
    }

    @Override
    public String toString() {
        return "+ AnnouncementBoard{ " + "key = '" + key + '\'' + " } ++++++++++++++++++++++++++++++++++++++\n" +
                "   Announcements: \n" + announcements + "\n" +
                "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++";
    }

}