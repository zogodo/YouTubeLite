package me.zogodo.youtubelite;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class CookieTool
{
    public static void SaveCookie(Context ctt, String cookieStr)
    {
        String filePath = ctt.getFilesDir().getPath() + "/cookie";
        Log.e("zzz Cookie ", filePath + " | " + cookieStr);
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

    public static String ReadCookie(Context ctt)
    {
        String filePath = ctt.getFilesDir().getPath() + "/cookie";
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
        return new String(fileContent);
    }
}
