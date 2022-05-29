package com.hcmute.lab_service_notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		EditText link = findViewById(R.id.txt_link);
		Button btn = findViewById(R.id.btn_download);
		btn.setOnClickListener(v -> handleDownload(link.getText().toString()) );
	}

	private void handleDownload(String link) {
		Intent intent = new Intent(this, DownloadService.getInstance().getClass());
		intent.putExtra("link", link);
		startService(intent);
	}
}