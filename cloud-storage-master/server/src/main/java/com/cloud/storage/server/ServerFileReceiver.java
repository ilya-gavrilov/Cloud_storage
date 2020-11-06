package com.cloud.storage.server;

import com.cloud.storage.common.FileMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ServerFileReceiver {
    private String finalFileName;
    private long finalFileLength;
    private File tempFile;
    private ConcurrentHashMap<Long, FileMessage> msgBuffer;
    private FileOutputStream fos;

    public ServerFileReceiver(String path, String name, long finalFileLength) throws IOException {
        tempFile = new File(path + "\\" + name + ".partial");
        tempFile.createNewFile();
        this.finalFileName = name;
        this.finalFileLength = finalFileLength;
        this.msgBuffer = new ConcurrentHashMap<>();
        this.fos = new FileOutputStream(tempFile);
    }

    public boolean receiveFile(FileMessage msg) throws IOException {
        while (!msgBuffer.isEmpty()) {
            long length = tempFile.length();
            FileMessage fm = msgBuffer.get(length);
            if (fm != null) {
                fos.write(fm.getData());
                msgBuffer.remove(length);
            } else break;
        }

        if (tempFile.length() == msg.getStartByte()) {
            fos.write(msg.getData());
        } else if (msg.getStartByte() > tempFile.length()) {
            msgBuffer.put(msg.getStartByte(), msg);
        }

        if (tempFile.length() == finalFileLength) {
            fos.close();
            tempFile.renameTo(new File(tempFile.getAbsolutePath().replace(".partial", "")));
            return true;
        }

        return false;
    }
}
