package com.cloud.storage.common;

import java.io.Serializable;

public class FileMessage implements Serializable {
    private String name;
    private long startByte;
    private byte[] data;

    public FileMessage(String name, long startByte, byte[] data) {
        this.name = name;
        this.startByte = startByte;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public long getStartByte() {
        return startByte;
    }

    public byte[] getData() {
        return data;
    }
}
