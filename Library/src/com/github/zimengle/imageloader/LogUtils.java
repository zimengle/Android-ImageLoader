package com.github.zimengle.imageloader;



import com.github.zimengle.BuildConfig;

import android.util.Log;



public class LogUtils {
	
	private static boolean isLog = BuildConfig.DEBUG;

	/**
	 * 关闭log输出
	 */
	public static void closeAllLogs() {
		isLog = false;
	}

	/**
	 * 打开log
	 */
	public static void openAllLogs() {
		isLog = true;
	}

	public static void d(String tag, String msg) {
		if (isLog) {
			Log.d(tag, msg);
		}
	}

	public static void e(String tag, String msg) {
		if (isLog) {
			Log.e(tag, msg);
		}
	}
	
	public static void e(String tag, String msg,Throwable e) {
		if (isLog) {
			Log.e(tag, msg,e);
		}
	}

	public static void v(String tag, String msg) {
		if (isLog) {
			Log.v(tag, msg);
		}
	}

	public static void w(String tag, String msg) {
		if (isLog) {
			Log.w(tag, msg);
		}
	}

	public static void i(String tag, String msg) {
		if (isLog) {
			Log.i(tag, msg);
		}
	}

	public static boolean isDebug() {
		return isLog;
	}
}
