package com.perez.netdiagnosis.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by xuzhou on 2016/9/12.
 * SharedPreferenceUtils
 */

public class SharedPreferenceUtils {

    public static void putInt(Context context, String key, int value) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putInt(key, value).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getInt(Context context, String key, int defaultValue) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            return sp.getInt(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static void putLong(Context context, String key, long value) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putLong(key, value).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long getLong(Context context, String key, long defaultValue) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            return sp.getLong(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static void putString(Context context, String key, String value) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putString(key, value).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getString(Context context, String key, String defaultValue) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            return sp.getString(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static void putBoolean(Context context, String key, boolean value) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putBoolean(key, value).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            return sp.getBoolean(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static void remove(Context context, String key) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().remove(key).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String Object2String(Object object) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            String string = new String(Base64.encode(byteArrayOutputStream.toByteArray(), Base64.DEFAULT));
            objectOutputStream.close();
            return string;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Object String2Object(String objectString) {
        byte[] mobileBytes = Base64.decode(objectString.getBytes(), Base64.DEFAULT);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mobileBytes);
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object object = objectInputStream.readObject();
            objectInputStream.close();
            return object;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static void save(Context context, String key, Object saveObject) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String string = Object2String(saveObject);
        editor.putString(key, string);
        editor.commit();
    }

    public static Object get(Context context, String key) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String string = sharedPreferences.getString(key, null);
        if (string != null) {
            Object object = String2Object(string);
            return object;
        } else {
            return null;
        }
    }
}
