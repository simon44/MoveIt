package dv606.sb223df.moveit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Simon on 06/05/2016.
 * Class with CRUD methods to manage the run in the database
 */
public class RunDataSource {

    // Database fields
    private SQLiteDatabase database;
    private RunDbHelper dbHelper;
    private String[] allColumns = { RunDbHelper.COLUMN_ID,
            RunDbHelper.COLUMN_COORDINATES,
            RunDbHelper.COLUMN_TIME,
            RunDbHelper.COLUMN_DISTANCE,
            RunDbHelper.COLUMN_DATE};

    public RunDataSource(Context context) {
        dbHelper = new RunDbHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Run createRun(String coordinates, int time, int distance, String date) {
        System.out.println("IN CREATE RUN METHOD.");
        ContentValues values = new ContentValues();
        values.put(RunDbHelper.COLUMN_COORDINATES, coordinates);
        values.put(RunDbHelper.COLUMN_TIME, time);
        values.put(RunDbHelper.COLUMN_DISTANCE, distance);
        values.put(RunDbHelper.COLUMN_DATE, date);
        long insertId = database.insert(RunDbHelper.RUN_TABLE_NAME, null, values);
        Cursor cursor = database.query(RunDbHelper.RUN_TABLE_NAME,
                allColumns, RunDbHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        Run newRun = cursorToRun(cursor);
        cursor.close();
        return newRun;
    }

    public void deleteRun(Run run) {
        long id = run.getId();
        System.out.println("Run deleted with id: " + id);
        database.delete(RunDbHelper.RUN_TABLE_NAME, RunDbHelper.COLUMN_ID
                + " = " + id, null);
    }

    public Run getRun(long runId) {
        String restrict = RunDbHelper.COLUMN_ID + "=" + runId;
        Cursor cursor = database.query(true, RunDbHelper.RUN_TABLE_NAME, allColumns, restrict,
                null, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            Run task = cursorToRun(cursor);
            return task;
        }
        // Make sure to close the cursor
        cursor.close();
        return null;
    }

    public boolean updateRun(long runId, String coordinates, int time, int distance, String date) {
        ContentValues args = new ContentValues();
        args.put(RunDbHelper.COLUMN_COORDINATES, coordinates);
        args.put(RunDbHelper.COLUMN_TIME, time);
        args.put(RunDbHelper.COLUMN_DISTANCE, distance);
        args.put(RunDbHelper.COLUMN_DATE, date);

        String restrict = RunDbHelper.COLUMN_ID + "=" + runId;
        return database.update(RunDbHelper.RUN_TABLE_NAME, args, restrict , null) > 0;
    }

    public List<Run> getAllRuns() {
        List<Run> runs = new ArrayList<Run>();
        Cursor cursor = database.query(RunDbHelper.RUN_TABLE_NAME,
                allColumns, null, null, null, null, RunDbHelper.COLUMN_ID + " DESC"); // Last runs first
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Run run = cursorToRun(cursor);
            runs.add(run);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return runs;
    }

    private Run cursorToRun(Cursor cursor) {
        Run run = new Run();
        run.setId(cursor.getInt(0));
        run.setCoordinates(cursor.getString(1));
        run.setTime(cursor.getInt(2));
        run.setDistance(cursor.getInt(3));
        run.setDate(cursor.getString(4));
        return run;
    }
}
