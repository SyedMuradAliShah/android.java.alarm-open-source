package com.example.alarmopensource;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private TextView alarmTimeTextView;
    private TextView currentTimeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Check and request permission for exact alarms on Android 12 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !getSystemService(AlarmManager.class).canScheduleExactAlarms()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(intent);
        }


        TimePicker timePicker = findViewById(R.id.timePicker);
        Button setAlarmButton = findViewById(R.id.setAlarmButton);
        alarmTimeTextView = findViewById(R.id.alarmTimeTextView);


        Calendar currentTime = Calendar.getInstance();
        currentTimeText = findViewById(R.id.currentTimeText);
        updateCurrentTimeTextView(currentTime);

        // It the number or identifier
        // for alarm 1 you can use 100 for alarm 2 you can use like 150 or whatever you want.
        // you can also use 1, for alarm 1, 2 for alarm 2 and so on
        Integer alarmIdentifier = 1;

        updateOldAlarmOnReopen(alarmIdentifier);

        setAlarmButton.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            calendar.set(Calendar.MINUTE, timePicker.getMinute());
            calendar.set(Calendar.SECOND, 0); // It will set 0 seconds, like 10:05:00 PM

             // TODO: This is truly for testing purpose... comment/remove, calendar.set(Calendar.SECOND, 0);
            // TODO: if you want to use calendar.add(Calendar.SECOND, 15)
//             calendar.add(Calendar.SECOND, 15); // Adds 10 seconds to the current time

            if (calendar.after(now)) {
                setAlarm(calendar.getTimeInMillis(), alarmIdentifier);
                updateAlarmTextView(calendar);

                Log.d("MainActivity", "Alarm set for: " + formatTime(calendar));
            } else {
                Toast.makeText(MainActivity.this, "Cannot set alarm for past time.", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Attempted to set an alarm for the past. Operation cancelled.");
            }
        });


        Button cancelAlarmButton = findViewById(R.id.cancelAlarmButton);
        cancelAlarmButton.setOnClickListener(v ->cancelAlarm(alarmIdentifier));

    }


    private void setAlarm(long timeInMillis, int identifier) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, identifier, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);

        // Save alarm set time to SharedPreferences
        getSharedPreferences(getPackageName()+"AlarmOpenSourceApp", MODE_PRIVATE)
                .edit()
                .putLong("alarmTime" + identifier, timeInMillis)
                .apply();
    }

    private void cancelAlarm(int identifier) {
        Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, identifier, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        // Clear saved alarm time from SharedPreferences
        getSharedPreferences(getPackageName() + "AlarmOpenSourceApp", MODE_PRIVATE)
                .edit()
                .remove("alarmTime" + identifier)
                .apply();

        alarmTimeTextView.setText("Alarm not set");
    }
    private void updateOldAlarmOnReopen(int alarmIdentifier){

        // Check if there is a saved alarm time and display it
        long savedAlarmTime = getSharedPreferences(getPackageName()+"AlarmOpenSourceApp", MODE_PRIVATE).getLong("alarmTime" + alarmIdentifier, 0);

        if (savedAlarmTime != 0) {
            // There is a saved alarm time, update the TextView
            Calendar alarmTime = Calendar.getInstance();
            alarmTime.setTimeInMillis(savedAlarmTime);
            updateAlarmTextView(alarmTime);
        }
    }

    private void updateCurrentTimeTextView(Calendar calendar){
        String alarmText = "Current Time: " + formatTime(calendar);
        currentTimeText.setText(alarmText);
    }

    private void updateAlarmTextView(Calendar calendar) {
        String alarmText = "Alarm set for: " + formatTime(calendar);
        alarmTimeTextView.setText(alarmText);
    }

    static String formatTime(Calendar calendar) {
        // Format the calendar time to a more readable form
        String timeFormat = "hh:mm:ss a"; // Example "03:00 PM"
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(timeFormat);
        return sdf.format(calendar.getTime());
    }



    // Just for tickering the time.

    private final Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            // Update the current time text view
            Calendar currentTime = Calendar.getInstance();
            updateCurrentTimeTextView(currentTime);

            // Post the ticker to run again after a delay of 1000ms (1 second)
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Start the ticker when the activity comes into the foreground
        ticker.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the ticker when the activity goes into the background
        handler.removeCallbacks(ticker);
    }
}