package ru.nsu.ccfit.zuev.osuplusplus;

import ru.nsu.ccfit.zuev.osuplusplus.GlobalManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.anddev.andengine.util.Debug;

public class ConfigBackup {

    public static boolean exportPreferences() {
        try {
            Context context = GlobalManager.getInstance().getMainActivity();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            
            JSONObject json = new JSONObject();
            Map<String, ?> allPrefs = prefs.getAll();
            
            allPrefs.entrySet()
                .stream()
                .filter(entry -> !Config.SENSITIVE_KEYS.contains(entry.getKey()))
                .forEach(entry -> {
                    try{
                        json.put(entry.getKey(), entry.getValue());
                    }catch(JSONException e){
                        Debug.e("ConfigBackup: " + e.getMessage(), e);
                    }
                });
            
            File backupFile = new File(Config.getCorePath(), "osudroid.cfg");
            try(FileOutputStream fos = new FileOutputStream(backupFile)) {
                fos.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                return true;
            }
        }catch(JSONException | IOException e) {
            Debug.e("ConfigBackup: " + e.getMessage(), e);
            return false;
        }
    }

    public static boolean importPreferences() {
        try {
            File backupFile = new File(Config.getCorePath(), "osudroid.cfg");
            if(!backupFile.exists()) {
                return false;
            }

            StringBuilder jsonBuilder = new StringBuilder();
            try(FileInputStream fis = new FileInputStream(backupFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while((bytesRead = fis.read(buffer)) != -1) {
                    jsonBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                }
            }
            
            JSONObject json = new JSONObject(jsonBuilder.toString());
            Context context = GlobalManager.getInstance().getMainActivity();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();

            for (var it = json.keys(); it.hasNext(); ) {
                String key = it.next();
                if (Config.SENSITIVE_KEYS.contains(key)) continue;
                Object value = json.get(key);

                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Double) {
                    editor.putFloat(key, ((Double) value).floatValue());
                } else {
                    editor.putString(key, value.toString());
                }
            }
            
            editor.apply();
            return true;
        }catch(JSONException | IOException e) {
            Debug.e("ConfigBackup: " + e.getMessage(), e);
            return false;
        }
    }

}

