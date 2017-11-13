package dv606.sb223df.moveit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Simon on 06/05/2016.
 * Method to create or update the database
 */
public class RunDbHelper extends SQLiteOpenHelper {

    public static final String RUN_TABLE_NAME = "run";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_COORDINATES = "coordinates";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_DISTANCE = "distance";
    public static final String COLUMN_DATE = "date";

    private static final String DATABASE_NAME = "run.db";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE = "create table " + RUN_TABLE_NAME
            + " (" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_COORDINATES + " text not null, "
            + COLUMN_TIME + " integer not null, "
            + COLUMN_DISTANCE + " integer not null, "
            + COLUMN_DATE + " text not null );";


    public RunDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(RunDbHelper.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + RUN_TABLE_NAME);
        onCreate(db);
    }
}
