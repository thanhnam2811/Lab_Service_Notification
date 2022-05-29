package com.hcmute.lab_service_notification;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static com.hcmute.lab_service_notification.DownloadBroadcastReceiver.ACTION;
import static com.hcmute.lab_service_notification.DownloadBroadcastReceiver.ACTION_CANCEL;
import static com.hcmute.lab_service_notification.DownloadBroadcastReceiver.ACTION_PAUSE;
import static com.hcmute.lab_service_notification.DownloadBroadcastReceiver.NOTIFICATION_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadService extends Service {
	public static final String
			CHANNEL_ID = "10001",
			CHANNEL_NAME = "Download Service",
			CHANNEL_DESCRIPTION = "Download Service";
	public static final String LINK = "link";

	static String SAVE_DIR = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();
	private static DownloadService instance;

	NotificationManager notificationManager;
	NotificationChannel channel;
	NotificationCompat.Builder builder;
	RemoteViews contentView;
	ExecutorService executor;
	Handler handler;
	boolean isPaused = false;

	// Get instance of DownloadService
	public static DownloadService getInstance() {
		if (instance == null) {
			instance = new DownloadService();
		}
		return instance;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String link = intent.getStringExtra(LINK);
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
		builder = new NotificationCompat.Builder(this, CHANNEL_ID);

		executor = Executors.newSingleThreadExecutor();
		handler = new Handler(getMainLooper());
		showNotification();
		executor.execute(() -> {
			downloadFile(link);
			handler.post(() -> {
				Toast.makeText(this, "Download completed", Toast.LENGTH_SHORT).show();
				notificationManager.cancel(Integer.parseInt(CHANNEL_ID));
			});
		});
		return START_NOT_STICKY;
	}

	public void downloadFile(String url) {
		// Log
		System.out.println("Downloading file from " + url);
		try {
			URL u = new URL(url);
			URLConnection conn = u.openConnection();
			int contentLength = conn.getContentLength();

			DataInputStream stream = new DataInputStream(u.openStream());
			String fileName = new File(Uri.parse(url).getPath()).getName();
			String path = SAVE_DIR + File.separator + fileName;
			File file = new File(path);

			int downloaded = 0;
			while (file.exists()) {
				String tempName = fileName.split("\\.")[0] + "(" + (++downloaded) + ")" + "." + fileName.split("\\.")[1];
				path = SAVE_DIR + File.separator + tempName;
				file = new File(path);
			}
			setMessage(file.getName());
			byte[] buffer = new byte[contentLength];
			int prev = 0, curr;
			for (int length = 0; length < contentLength; length += stream.read(buffer, length, contentLength - length)) {
				curr = length * 100 / contentLength;
				if (curr - prev >= 1) {
					prev = curr;
					setProgress(curr);
				}
			}

			DataOutputStream fos = new DataOutputStream(new FileOutputStream(file));
			fos.write(buffer);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setMessage(String message) {
		contentView.setTextViewText(R.id.message, message);
		builder.setContent(contentView);
		Notification notification = builder.build();
		notificationManager.notify(Integer.parseInt(CHANNEL_ID), notification);
	}

	private void setProgress(int progress) {
		contentView.setViewVisibility(R.id.progress, View.VISIBLE);
		contentView.setProgressBar(R.id.progress, 100, progress, false);
		builder.setContent(contentView);
		Notification notification = builder.build();
		notificationManager.notify(Integer.parseInt(CHANNEL_ID), notification);
	}

	private void showNotification() {
		channel.setDescription(CHANNEL_DESCRIPTION);

		notificationManager.createNotificationChannel(channel);

		contentView = new RemoteViews(getPackageName(), R.layout.notification_download);
		contentView.setTextViewText(R.id.title, "Downloading");
		contentView.setOnClickPendingIntent(R.id.cancel, getCancelPendingIntent());
		contentView.setOnClickPendingIntent(R.id.pause, getPausePendingIntent());

		builder.setSmallIcon(R.mipmap.ic_launcher_round);
		builder.setPriority(NotificationCompat.PRIORITY_HIGH);
		builder.setOnlyAlertOnce(true);
		builder.setContent(contentView);

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		builder.setContentIntent(pendingIntent);

		Notification notification = builder.build();
		notificationManager.notify(Integer.parseInt(CHANNEL_ID), notification);
	}

	private PendingIntent getPausePendingIntent() {
		Intent pauseIntent = new Intent(this, DownloadBroadcastReceiver.class);
		pauseIntent.putExtra(NOTIFICATION_ID, CHANNEL_ID);
		pauseIntent.putExtra(ACTION, ACTION_PAUSE);
		return PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
	}

	private PendingIntent getCancelPendingIntent() {
		Intent cancelIntent = new Intent(this, DownloadBroadcastReceiver.class);
		cancelIntent.putExtra(NOTIFICATION_ID, CHANNEL_ID);
		cancelIntent.putExtra(ACTION, ACTION_CANCEL);
		return PendingIntent.getBroadcast(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void pauseDownload() throws InterruptedException {
		if (isPaused) {
			isPaused = false;
			executor.notify();
			contentView.setTextViewText(R.id.pause, "Pause");
		} else {
			isPaused = true;
			executor.wait();
			contentView.setTextViewText(R.id.pause, "Resume");
		}
		builder.setContent(contentView);
		Notification notification = builder.build();
		notificationManager.notify(Integer.parseInt(CHANNEL_ID), notification);
	}

	public void cancelDownload() {
		notificationManager.cancel(Integer.parseInt(CHANNEL_ID));
	}
}
