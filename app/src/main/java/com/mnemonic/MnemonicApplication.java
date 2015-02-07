package com.mnemonic;


import android.app.Application;

import com.mnemonic.db.DbHelper;


public class MnemonicApplication extends Application {

    private static DbHelper DB_HELPER;

    @Override
    public void onCreate() {
        super.onCreate();

        DB_HELPER = new DbHelper(getApplicationContext());
    }

    public static DbHelper getDbHelper() {
        return DB_HELPER;
    }
}
