package com.cloud.storage.client;

public class RemoteFileListItem {

    private String name;
    private int sizeKB;
    private long sizeByte;

    public RemoteFileListItem(String name, long sizeByte) {
        this.name = name;
        this.sizeByte = sizeByte;
        this.sizeKB = (int) (sizeByte / 1024);
    }

    public String getName() {
        return name;
    }

    public long getSizeByte() {
        return sizeByte;
    }

    public int getSizeKB() {
        return sizeKB;
    }
}
