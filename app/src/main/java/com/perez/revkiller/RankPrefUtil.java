package com.perez.revkiller;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class RankPrefUtil {

    private SharedPreferences sharedPreferences;

    public RankPrefUtil(Context ctx) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    private void SetKeyBool(String key, boolean val) {
        SharedPreferences.Editor edt = sharedPreferences.edit();
        edt.putBoolean(key, val);
        edt.commit();
    }

    public void SetReverse(boolean val) {
        SetKeyBool("revrank", val);
    }

    public boolean GetReverse() {
        return sharedPreferences.getBoolean("revrank", false);
    }

    public void SetByName() {
        SetKeyBool("nombre", true);
        SetKeyBool("tipo", false);
        SetKeyBool("fecha", false);
        SetKeyBool("tamano", false);
    }

    public void SetByType() {
        SetKeyBool("nombre", false);
        SetKeyBool("tipo", true);
        SetKeyBool("fecha", false);
        SetKeyBool("tamano", false);
    }

    public void SetByDate() {
        SetKeyBool("nombre", false);
        SetKeyBool("tipo", false);
        SetKeyBool("fecha", true);
        SetKeyBool("tamano", false);
    }

    public void SetBySize() {
        SetKeyBool("nombre", false);
        SetKeyBool("tipo", false);
        SetKeyBool("fecha", false);
        SetKeyBool("tamano", true);
    }

    public String GetWhich() {
        boolean b1 = sharedPreferences.getBoolean("tipo", false);
        boolean b2 = sharedPreferences.getBoolean("fecha", false);
        boolean b3 = sharedPreferences.getBoolean("tamano", false);
        boolean b4 = sharedPreferences.getBoolean("nombre", false);
        if(b1) return "type";
        else if(b2) return "date";
        else if(b3) return "size";
        else if(b4) return "name";
        else return "null";
    }
}
