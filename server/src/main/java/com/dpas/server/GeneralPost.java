package com.dpas.server;

import java.util.*;
import com.dpas.HelloWorld;
import java.io.Serializable;

public class GeneralPost implements Serializable {
    private ArrayList<Post> posts;
    private byte[] signature;
    private byte[] hash;

    public GeneralPost(){
        this.posts = new ArrayList<Post>();
    }
    public ArrayList<Post> getPosts() {
        return posts;
    }

    public void setPosts(ArrayList<Post> posts) {
        this.posts = posts;
    }

    public void addPosts(Post posts) {
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