package dv606.sb223df.moveit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by Simon on 19/05/2016.
 * Activity that shows the history, i.e the list of runs registred
 */
public class HistoryActivity extends AppCompatActivity {

    private ListView listView;
    private RunHistoryAdapter adapter;
    private RunDataSource datasource;
    private List<Run> listRuns;
    private String DISTANCE_UNIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Manage preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        DISTANCE_UNIT = sp.getString("distance_preference", getString(R.string.meter));

        datasource = new RunDataSource(this);
        listView = (ListView)findViewById(R.id.listView);
        adapter = new RunHistoryAdapter(this, R.layout.runhistory_list_item);
        listView.setAdapter(adapter);

        /* Attach context menu on list */
        registerForContextMenu(listView);
        refreshDataFromDB();
    }

    class RunHistoryAdapter extends ArrayAdapter<Run> {

        public RunHistoryAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override   // Called when updating the ListView
        public View getView(int position, View convertView, ViewGroup parent) {
            /* Reuse super handling ==> A TextView from R.runhistory_list_item */
            View row;
            if (convertView == null) {	// Create new row view object
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.runhistory_list_item, parent, false);
            }
            else {
                row = convertView;
            }

            // Parse the line
            Run run = getItem(position);
            if (run == null) {
                System.out.println("ERROR, RUN IS NULL");
            } else {
                TextView idTV = (TextView) row.findViewById(R.id.run_id);
                idTV.setText(String.valueOf(run.getId()));

                TextView dateTV = (TextView) row.findViewById(R.id.run_date);
                dateTV.setText(run.getDate());

                TextView timeTV = (TextView) row.findViewById(R.id.run_time);
                timeTV.setText(String.valueOf(DateUtils.formatElapsedTime(run.getTime())));

                // Get the distance unit to set the unit in the textview and compute the right distance to display
                double multiplicator = 0.0;
                DecimalFormat df = null;
                if (DISTANCE_UNIT.equals(getString(R.string.meter))) {
                    multiplicator = 1;
                    df = new DecimalFormat("0");
                } else if (DISTANCE_UNIT.equals(getString(R.string.kilometer))) {
                    multiplicator = 0.001;
                    df = new DecimalFormat("##.###");
                }

                TextView distanceTV = (TextView) row.findViewById(R.id.run_distance);
                distanceTV.setText(String.valueOf(df.format(run.getDistance() * multiplicator)) + " " + DISTANCE_UNIT);
            }
            return row;
        }
    }

    /**
     * Method to get runs from database and populate the list with them
     */
    private void refreshDataFromDB() {
        datasource.open();
        listRuns = datasource.getAllRuns();
        adapter.clear();
        for (Run r : listRuns) {
            adapter.add(r);
        }
        adapter.notifyDataSetChanged();
    }

    /*
     *  Adding context menus for showing a run, share it or delete it
     */
    public static final int SHOW = 0;
    public static final int SHARE = 1;
    public static final int DELETE_ONE = 2;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Action to do");
        menu.add(0, SHOW, 0, "Show details");
        menu.add(0, SHARE, 0, "Share");
        menu.add(0, DELETE_ONE, 0, "Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info= (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        LinearLayout row = (LinearLayout) info.targetView;
        long id;
        switch (item.getItemId()) {

            case SHOW:
                id = Long.valueOf(((TextView) row.findViewById(R.id.run_id)).getText().toString());
                Intent intent = new Intent(this, SingleRunHistoryActivity.class);
                intent.putExtra("run_id", id); // We start the SingleHistory Activity with the right run id
                startActivity(intent);
                return true;

            case DELETE_ONE:
                RunDataSource datasource = new RunDataSource(this);
                datasource.open();
                Run runToDelete = new Run(); // just need the ID to delete
                id = Long.valueOf(((TextView) row.findViewById(R.id.run_id)).getText().toString());
                runToDelete.setId(id);
                datasource.deleteRun(runToDelete);
                datasource.close();
                refreshDataFromDB();
                return true;

            case SHARE: // Share with ACTION_SEND
                String date = ((TextView) row.findViewById(R.id.run_date)).getText().toString();
                String time = ((TextView) row.findViewById(R.id.run_time)).getText().toString();
                String distance = ((TextView) row.findViewById(R.id.run_distance)).getText().toString();

                String toShare = "Hey ! I runned " + distance + "m in " + time + " seconds the " + date;
                Intent intentSend = new Intent(Intent.ACTION_SEND);
                String title = "SELECT AN APP TO SHARE";
                // Create intent to show chooser
                Intent chooser = Intent.createChooser(intentSend, title);
                intentSend.setType("text/plain");
                intentSend.putExtra(Intent.EXTRA_TEXT, toShare);
                // Verify the intent will resolve to at least one activity
                if (intentSend.resolveActivity(getPackageManager()) != null) {
                    System.out.println("Start chooser send");
                    startActivity(chooser);
                }
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.choice1 : // Reset the history
                RunDataSource datasource = new RunDataSource(this);
                datasource.open();
                List<Run> listRuns = datasource.getAllRuns();
                for (Run r : listRuns) {
                    datasource.deleteRun(r);
                }
                datasource.close();
                refreshDataFromDB();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        datasource.open();
        super.onResume();
    }

    @Override
    protected void onPause() {
        datasource.close();
        super.onPause();
    }
}
