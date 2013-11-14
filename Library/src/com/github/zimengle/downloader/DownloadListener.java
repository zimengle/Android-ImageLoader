package com.github.zimengle.downloader;

import java.net.HttpURLConnection;

public interface DownloadListener {
	public void start(HttpURLConnection connection);
	public void transfer(long loaded,long total,HttpURLConnection connection);
	public void success(HttpURLConnection connection);
	public void cancel(HttpURLConnection connection);
	public static class SimpleDownloadListener implements DownloadListener{

		public void start(HttpURLConnection connection) {
		}

		public void transfer(long loaded,long total, HttpURLConnection connection) {
		}

		public void success(HttpURLConnection connection) {
		}

		public void cancel(HttpURLConnection connection) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
