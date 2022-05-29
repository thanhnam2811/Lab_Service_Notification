package com.hcmute.lab_service_notification;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class DownloadBroadcastReceiver extends BroadcastReceiver {
	public static final String
			NOTIFICATION_ID = "notification_id",
			ACTION = "action",
			ACTION_CANCEL = "action_cancel",
			ACTION_PAUSE = "action_pause";

	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationManager manager = (NotificationManager)
				context.getSystemService(Context.NOTIFICATION_SERVICE);
		String notificationId = intent.getStringExtra(NOTIFICATION_ID);

		String action = intent.getStringExtra(ACTION);
		System.out.println("action: " + action);
		if (ACTION_CANCEL.equals(action)) {
			DownloadService.getInstance().executor.shutdownNow();
		} else if (ACTION_PAUSE.equals(action)) {
			try {
				DownloadService.getInstance().pauseDownload();
				Toast.makeText(context, "Pause download", Toast.LENGTH_SHORT).show();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
