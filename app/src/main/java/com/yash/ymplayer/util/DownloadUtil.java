package com.yash.ymplayer.util;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadUtil {
    public static byte[] download(String url) throws IOException {
        URL web = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) web.openConnection();
        connection.connect();
        InputStream stream = connection.getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[1048576];
        int count;
        while ((count = stream.read(bytes))!=-1) {
            outputStream.write(bytes,0,count);
        }
        return outputStream.toByteArray();
    }
}
