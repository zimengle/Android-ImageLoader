package com.github.zimengle.imageloader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import com.github.zimengle.downloader.DefaultDownloader;
import com.github.zimengle.downloader.DownloadListener;
import com.github.zimengle.downloader.Downloader;
import com.github.zimengle.downloader.UniqueURLDownloader;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;

/**
 * Http图片类,负责加载http传输协议下的图片
 * @author zhangzimeng<zhangzimeng01@baidu.com>
 */
public class HttpImage extends Image{

	private HttpURLConnection conn;
	
	private Context context;
	
	private Size size;
	
	private File file;
	
	private Downloader downloader;
	
	private Image image;
	
	private Options options;
	
	private boolean cancel = false;
	
	private DownloadListener downloadListener;
	
	/**
	 * 构造器
	 * @param context
	 * @param file 下载位置
	 * @param conn 
	 * @param size 加载尺寸
	 * @param options 图片选项
	 */
	public HttpImage(Context context,HttpURLConnection conn,File file,Size size,Options options) {
		if(options == null){
			options = new Options();
		}
		if(file == null){
			file = new File(ImageCache.getDiskCacheDir(context,"http"),""+conn.getURL().toString().hashCode());
		}
		this.conn = conn;
		this.file = file;
		this.context = context;
		this.size = size;
		this.options = options;
	}
	
	
	public void setDownloadListener(DownloadListener downloadListener) {
		this.downloadListener = downloadListener;
	}
	
	public HttpImage(Context context,HttpURLConnection conn,File file,Size size){
		this(context,conn,file,size,null);
	}
	
	public HttpImage(Context context,HttpURLConnection conn,File file){
		this(context,conn,file,null,null);
	}
	
	public HttpImage(Context context,HttpURLConnection conn){
		this(context,conn,null,null);
	}
	
	@Override
	protected Bitmap generateImage() throws IOException {
		Bitmap bitmap = null;
		if(!cancel){
			downloader = new UniqueURLDownloader(new DefaultDownloader(conn, file));
			downloader.setDownloadListener(downloadListener);
			if(downloader.download()){
				 image = new LocalImage(context, file, size,options);
				 bitmap = image.getImage();
			}
		}
		
		
		return bitmap;
	}

	@Override
	public void cancel() {
		cancel = true;
		if(downloader != null){
			downloader.cancel();
		}
		if(image != null){
			image.cancel();
		}
	}


	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return getURL().toString();
	}
	
	public URL getURL(){
		return conn.getURL();
	}


}
