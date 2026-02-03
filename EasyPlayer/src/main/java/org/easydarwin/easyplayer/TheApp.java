package org.easydarwin.easyplayer;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import androidx.multidex.MultiDexApplication;

import org.easydarwin.easyplayer.data.EasyDBHelper;
import org.easydarwin.video.Client;

public class TheApp extends MultiDexApplication {

    public static SQLiteDatabase sDB;

    @Override
    public void onCreate() {
        super.onCreate();

        sDB = new EasyDBHelper(this).getWritableDatabase();
    }
}
