package com.github.zimengle.downloader;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * 下载图片
 * @author zhangzimeng<zhangzimeng01@baidu.com>
 */
public interface Downloader {
	
	/**
	 * 下载下载
	 * @return 是否下载完成
	 * @throws IOException
	 */
	public boolean download() throws IOException;
	
	/**
	 * 取消下载
	 */
	public void cancel();
	
	/**
	 * 获取下载的URL
	 * @return URL
	 */
	public String getURL();
	
	public void setDownloadListener(DownloadListener downloadListener);
	
}
