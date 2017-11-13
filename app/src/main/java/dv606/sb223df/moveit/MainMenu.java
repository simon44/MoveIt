package dv606.sb223df.moveit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Main menu is the activity show at start, allow user to select the action to do
 */
public class MainMenu extends AppCompatActivity {

    private Activity main_activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        main_activity = this;

        Button startRunButton = (Button) findViewById(R.id.start_run);
        Button showHistoryButton = (Button) findViewById(R.id.show_history);
        Button quitAppButton = (Button) findViewById(R.id.quit_app);
        Button settingsButton = (Button) findViewById(R.id.settings);
        startRunButton.setOnClickListener(new StartRunButtonClick());
        showHistoryButton.setOnClickListener(new ShowHistoryButtonClick());
        quitAppButton.setOnClickListener(new QuitAppButtonClick());
        settingsButton.setOnClickListener(new SettingsButtonClick());
    }

    private class StartRunButtonClick implements View.OnClickListener {
        public void onClick(View v) {
            System.out.println("Start Run.");
            Intent intent = new Intent(main_activity, RunActivity.class);
            startActivity(intent);
        }
    }

    private class ShowHistoryButtonClick implements View.OnClickListener {
        public void onClick(View v) {
            System.out.println("Show History.");
            Intent intent = new Intent(main_activity, HistoryActivity.class);
            startActivity(intent);
        }
    }

    private class SettingsButtonClick implements View.OnClickListener {
        public void onClick(View v) {
            System.out.println("Show Settings.");
            Intent intent = new Intent(main_activity, SettingsActivity.class);
            startActivity(intent);
        }
    }

    private class QuitAppButtonClick implements View.OnClickListener {
        public void onClick(View v) {
            System.out.println("Quit App.");
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("Move it application finished.");
    }
}
