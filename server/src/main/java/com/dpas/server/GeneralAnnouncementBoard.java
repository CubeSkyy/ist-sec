package com.dpas.server;

import java.util.*;
import java.text.SimpleDateFormat;

public class GeneralAnnouncementBoard {
    private static GeneralAnnouncementBoard mInstance = null;
    public List<Announcement> announcements;

    private GeneralAnnouncementBoard(){
        this.announcements = new ArrayList<Announcement>();
    }

    public static GeneralAnnouncementBoard getInstance(){
        if(mInstance == null){
            mInstance = new GeneralAnnouncementBoard();
        }
        return mInstance;
    }
    public List<Announcement> getAnnouncements(){
        return this.announcements;
    }

    public void addAnnouncement(Announcement a){
        this.announcements.add(a);
    }

    @Override
    public String toString() {
        return "+ GeneralAnnouncementBoard +++++++++++++++++++++++++++++++++++++++++++++++++++++\n" +
                "   Announcements: \n" + announcements + "\n" +
                "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++";
    }

}