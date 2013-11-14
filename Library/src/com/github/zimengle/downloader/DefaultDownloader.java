package com.github.zimengle.downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import com.github.zimengle.downloader.TempMetaFile.Meta;
import com.github.zimengle.imageloader.LogUtils;

/**
 * 默认下载器,支持断点续传功能
 * @author zhangzimeng<zhangzimeng01@baidu.com>
 *
 */
public class DefaultDownloader implements Downloader{
	
	private static final String TAG = "DefaultDownloader";
	
	private static final int BUFFER = 1024*1024;
	
	private File tempFile;
	
	private File diskFile;
	
	private HttpURLConnection conn;
	
	private DownloadListener downloadListener;
	
	private TempMetaFile tempMetaFile;
	
	public DefaultDownloader(HttpURLConnection conn,File diskFile) {
		this.tempFile = new File(diskFile+".temp");
		this.diskFile = diskFile;
		this.conn  = conn;
		this.tempMetaFile = TempMetaFile.getInstance(new File(diskFile.getParent(),"mengleloader.meta"));
	}
	
	public void setDownloadListener(DownloadListener downloadListener) {
		this.downloadListener = downloadListener;
	}

	/**
	 * 取消下载
	 */
	public void cancel() {
		conn.disconnect();
		fireCancel(conn);
		LogUtils.d(TAG, "cancel:"+getURL());
	}
	
	
	/**
	 * 下载图片
	 */
	public boolean download() throws IOException {
		//如果已经下载,则直接返回
		if(diskFile.exists()){
			LogUtils.d(TAG, "hit disk cache:"+getURL());
			return true;
		}
		if(!tempFile.getParentFile().exists()){
			tempFile.getParentFile().mkdirs();
		}
		Meta meta = null;
		if(!tempFile.exists()){
			tempFile.createNewFile();
		}else{
			meta = tempMetaFile.get(getURL());
		}
		
		long startPos = tempFile.length();
		Range range = new Range(startPos);
		fireStart(conn);
		//设置下载范围的头信息
		conn.setRequestProperty("Range", range.getRangeHeader());
		conn.connect();
		LogUtils.d(TAG, "fetch start:"+getURL());
		int status = conn.getResponseCode();
		boolean append = false;
		Meta newMeta = new Meta(conn.getHeaderField("ETag"), conn.getLastModified(), conn.getContentLength());
		if(status == 200){
			append = false;
		//如果206并且文件完整性一致
		}else if(status == 206 && (meta == null || newMeta.check(meta))){
			LogUtils.d(TAG, "temp continue:"+getURL());
			append = true;
		}else{
			return false;
		}
		tempMetaFile.put(getURL(), newMeta);
		long loaded = startPos,total = conn.getContentLength()+loaded;
		int n = -1;
		byte[] buffer = new byte[BUFFER];
		InputStream input = null;
		FileOutputStream output = null;
		try{
			input = conn.getInputStream();
			output = new FileOutputStream(tempFile, append);
			while ( (n=input.read(buffer)) != -1) {
				output.write(buffer,0,n);
				loaded += n;
				fireTransfer(loaded, total, conn);
			}
			tempFile.renameTo(diskFile);
			fireSuccess(conn);
			tempMetaFile.remove(getURL());
			LogUtils.d(TAG, "fetch complete:"+getURL());
			return true;
		}catch(IOException e){
			throw e;
		}finally{
			if(input != null){
				input.close();
			}
			if(output != null){
				output.close();
			}
			if(conn != null){
				conn.disconnect();
			}
			
		}
	}


	/**
	 * 获取下载的URL
	 */
	public String getURL() {
		return conn.getURL().toString();
	}

	public void fireStart(HttpURLConnection connection) {
		if(downloadListener != null){
			downloadListener.start(connection);
		}
		
	}

	public void fireTransfer(long loaded,long total, HttpURLConnection connection) {
		if(downloadListener != null){
			downloadListener.transfer(loaded, total,conn);
		}
		
	}

	public void fireSuccess(HttpURLConnection connection) {
		if(downloadListener != null){
			downloadListener.success(connection);
		}
		
	}

	public void fireCancel(HttpURLConnection connection) {
		if(downloadListener != null){
			downloadListener.success(connection);
		}
		
	}

	

}
