package me.zogodo.youtubelite;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class CookieTool
{
    public static String RawFileToString(Context act, int file_id)
    {
        //读取Raw文件成字符串
        InputStream is = act.getResources().openRawResource(file_id);
        byte[] buffer = new byte[65536];
        int count = 0;
        try
        {
            count = is.read(buffer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return new String(Arrays.copyOfRange(buffer, 0, count));
    }

    public static void SaveCookie(Context ctt, String cookieStr, String domain)
    {
        String filePath = ctt.getFilesDir().getPath() + "/" + domain;
        Log.e("zzz Cookie w", filePath + " | " + cookieStr);
        if (cookieStr == null)
        {
            return;
        }
        BufferedWriter writer;
        try
        {
            writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(cookieStr);
            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static String ReadCookie(Context ctt, String domain)
    {
        String filePath = ctt.getFilesDir().getPath() + "/" + domain;
        File file = new File(filePath);
        long fileLength = file.length();
        byte[] fileContent = new byte[(int)fileLength];
        try
        {
            FileInputStream in = new FileInputStream(file);
            in.read(fileContent);
            in.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        String cookieStr = new String(fileContent);
        Log.e("zzz Cookie r", filePath + " | " + cookieStr);
        return cookieStr;
    }
}
