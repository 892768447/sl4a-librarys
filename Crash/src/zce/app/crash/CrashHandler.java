package zce.app.crash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import zce.app.sdpath.GetPath;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.InflateException;
import android.widget.Toast;

@SuppressLint("NewApi")
public class CrashHandler implements UncaughtExceptionHandler {
	private static String TAG = "CrashHandler";
	private Context mContext;
	private String msg = "啊哦！程序出了点小问题!";
	private boolean restart = false;
	private Thread.UncaughtExceptionHandler mDefaultHandler;
	private static CrashHandler INSTANCE;
	private Map<String, String> infos = new HashMap<String, String>();

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		if (!handleException(ex) && mDefaultHandler != null) {
			mDefaultHandler.uncaughtException(thread, ex);
		} else {

			new Thread() {
				@Override
				public void run() {
					try {
						Looper.prepare();
						Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
						Looper.loop();
					} catch (InflateException e) {

					}
				}
			}.start();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (restart) {
				Intent intent = mContext.getPackageManager()
						.getLaunchIntentForPackage(mContext.getPackageName());
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(intent);
			}
			android.os.Process.killProcess(android.os.Process.myPid());
			System.exit(0);
		}
	}

	private boolean handleException(Throwable ex) {
		if (ex == null) {
			return false;
		}
		ex.printStackTrace();
		collectDeviceInfo(mContext);

		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, String> entry : infos.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (!key.equals("TIME")) {
				sb.append(key + "=" + value + "\n");
			}
		}

		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		ex.printStackTrace(printWriter);
		Throwable cause = ex.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		printWriter.close();
		String result = writer.toString();
		sb.append(result);

		// todo
		WriteSettings(mContext, sb.toString(), "error");

		return true;
	}

	private void collectDeviceInfo(Context ctx) {
		Field[] fields = Build.class.getDeclaredFields();
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				infos.put(field.getName(), field.get(null).toString());
				Log.d(TAG, field.getName() + " : " + field.get(null));
			} catch (Exception e) {
				Log.e(TAG, "an error occured when collect crash info", e);
			}
		}

	}

	public void init(Context ctx, String msg, boolean restart) {
		this.msg = msg;
		this.restart = restart;
		mContext = ctx.getApplicationContext();
		mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	public void init(Context ctx, int id, boolean restart) {
		this.restart = restart;
		mContext = ctx.getApplicationContext();
		this.msg = mContext.getString(id);
		mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	public static CrashHandler getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new CrashHandler();
		}
		return INSTANCE;
	}

	private static void WriteSettings(Context context, String data, String name) {

		File log = new File(new GetPath().path(context),
				context.getPackageName() + "_last_err.log");
		if (log.exists()) {
			log.delete();
		}
		byte[] datas = data.getBytes();
		FileOutputStream outStream;
		try {
			outStream = new FileOutputStream(log);
			outStream.write(datas);
			outStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}