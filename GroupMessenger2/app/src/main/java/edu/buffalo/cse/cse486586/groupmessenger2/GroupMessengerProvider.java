package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class GroupMessengerProvider extends ContentProvider {
    public static final String PREFS_NAME = "MyPrefsFile";
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }


    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }



    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(values.getAsString("key"),values.getAsString("value"));
        editor.apply();



        //Log.v("insert", values.toString());

        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, 0);

        //Log.v("query", selection);

        String value = prefs.getString(selection,"");
        String cnames[]={"key","value"};
        MatrixCursor matrixCursor = new MatrixCursor(cnames,2);
        String keyvalue[] = {selection,value};
        matrixCursor.addRow(keyvalue);
        return matrixCursor;
    }
}
