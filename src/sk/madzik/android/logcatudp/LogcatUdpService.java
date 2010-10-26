package sk.madzik.android.logcatudp;

import java.net.DatagramSocket;
import java.net.SocketException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;

public class LogcatUdpService extends Service {
	public static final String TAG = "LogcatUdpService";
	public static boolean isRunning = false;

	class Config {
		boolean mSendIds;
		String mDevId;
		String mDestServer;
		int mDestPort;
	}
	private Config mConfig = null;

	private DatagramSocket mSocket = null;
	private LogcatThread mLogcatThread = null;

	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(TAG, TAG+" started");

		// get configuration
		mConfig = new Config();
		SharedPreferences settings = getSharedPreferences(LogcatUdpCfg.Preferences.PREFS_NAME, Context.MODE_PRIVATE);
		mConfig.mSendIds = settings.getBoolean(LogcatUdpCfg.Preferences.SEND_IDS, false);
		String android_ID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
		if (TextUtils.isEmpty(android_ID))
			android_ID = "emulator";
		mConfig.mDevId = settings.getString(LogcatUdpCfg.Preferences.DEV_ID, android_ID);
		mConfig.mDestServer = settings.getString(LogcatUdpCfg.Preferences.DEST_SERVER, LogcatUdpCfg.DEF_SERVER);
		mConfig.mDestPort = settings.getInt(LogcatUdpCfg.Preferences.DEST_PORT, LogcatUdpCfg.DEF_PORT);

		// TODO: notification on statusbar

		// TODO: read logcat, open udp port, send lines (thread)
		try {
			mSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			Log.e(TAG, "Socket creation failed!");
			stopSelf();
		}
		mLogcatThread  = new LogcatThread( mSocket, mConfig );
		mLogcatThread.start();

		isRunning = true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, TAG+" stopping.");
		if ( mLogcatThread != null ) {
			mLogcatThread.interrupt();
			try {
				mLogcatThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				Log.w(TAG, "Joining logcat thread exception.");
			}
		}
		isRunning = false;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}