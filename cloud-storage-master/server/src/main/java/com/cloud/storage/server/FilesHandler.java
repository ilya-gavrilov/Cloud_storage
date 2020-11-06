package com.cloud.storage.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class FilesHandler {

    public static ArrayList<String[]> listDirectory(String path) {
        ArrayList filesList = new ArrayList();
        for (File file : new File(path).listFiles()) {
            filesList.add(new String[]{file.getName(), Long.toString(file.length())});
        }
        return filesList;
    }

    public static long getAvailableSize(String path) {
        long totalLength = 0;
        for (File currentFile : new File(path).listFiles()) {
            totalLength += currentFile.length();
        }

        return SettingsMgmt.MAX_USER_FOLDER_SIZE - totalLength;
    }

    public static boolean isFileExist(String path, String name) {
        boolean isExist = false;
        for (File file : new File(path).listFiles()) {
            if (file.getName().equals(name)) {
                return true;
            }
        }

        return isExist;
    }

    public static long getFileLength(String path, String name) {
        return (new File(path + "\\" + name)).length();
    }

    public static boolean deleteFile(String path, String name) {
        return (new File(path + "//" + name)).delete();
    }

    public static boolean renameFile(String path, String name, String newName) {
        return (new File(path + "\\" + name)).renameTo(new File(path + "\\" + newName));
    }
}
