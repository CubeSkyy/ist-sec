package com.dpas.server;

import java.util.*;
import com.dpas.HelloWorld;
import java.io.Serializable;

public class Post implements Serializable {
    private ArrayList<HelloWorld.Announcement> posts;
    private byte[] signature;
    private byte[] hash;

    public Post(){
        this.posts = new ArrayList<HelloWorld.Announcement>();
    }
    public ArrayList<HelloWorld.Announcement> getPosts() {
        return posts;
    }

    public void setPosts(ArrayList<HelloWorld.Announcement> posts) {
        this.posts = posts;
    }

    public void addPosts(HelloWorld.Announcement posts) {
        this.posts.add(posts);
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "Post{" +
                "posts=" + posts +
                ", signature=" + java.util.Arrays.toString(signature) +
                ", hash=" + java.util.Arrays.toString(hash) +
                '}';
    }
}