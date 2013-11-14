package com.github.zimengle.imageloader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.github.zimengle.imageloader.Image.Size;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver.OnDrawListener;
import android.widget.ImageView;

/**
 * 图片加载器,支持多线程并发加载,支持暂停任务
 * @author zhangzimeng<zhangzimeng01@baidu.com>
 *
 */
public class ImageLoader {

	private static final String TAG = "ImageLoader";

	//任务队列,多线程会同步获取该队列执行
	private List<WorkItem> queue = new LinkedList<WorkItem>();
	
	//用于视图,任务映射,特别是在listview,gridview对象复用的时候,及时终止之前的任务
	private Map<View, WorkItem> map = new HashMap<View, ImageLoader.WorkItem>();
	
	//线程池队列
	private WorkThread[] threadPool;

	private Handler handler = new Handler();
	
	//图片缓存对象
	private ImageCache imageCache;

	//全局loading提示
	private Drawable loadBitmap;

	//全局resize的大小
	private Size size;

	private Context context;
	
	private boolean pause;
	
	private File httpCacheDir;
	
	private Options bitmapOptions;
	
	/**
	 * 任务线程内部类
	 * @author zhangzimeng<zhangzimeng01@baidu.com>
	 *
	 */
	private class WorkThread extends Thread {
		
		//停止线程的标示
		private boolean stop = false;

		//当前任务
		private WorkItem workItem;
		
		public WorkThread() {
			//优先级调至低一级,避免争夺UI线程,导致卡顿
			setPriority(NORM_PRIORITY - 1);
		}

		@Override
		public void run() {
			while(!stop){
				//获取任务队列,木有就主动睡掉
				synchronized (queue) {
					if(queue.size() == 0 || pause){
						try {
							queue.wait();
						} catch (InterruptedException e) {
						}
						continue;
						
					}
					workItem = queue.remove(0);
				}
				try{
					workItem.run();
				}catch(Exception e){
					LogUtils.e(TAG, "error",e);
					
				}
				workItem = null;
				continue;
				
			}
		}
		
		/**
		 * 停止线程
		 */
		public void quit(){
			stop = true;
			if(workItem != null){
				workItem.cancel();
			}
		}
		
		
	}

	/**
	 * 任务内部类
	 * @author zhangzimeng<zhangzimeng01@baidu.com>
	 */
	private class WorkItem  {

		private ImageView imageView;

		private Image image;

		private Runnable uiRunnable;
		
		private Options options;
		
		private boolean cancel = false;
		
		private LoadListener loadListener;
		
		public WorkItem(ImageView imageView, File localFile,Size size,Options options,LoadListener loadListener) {
			if(size == null){
				size = ImageLoader.this.size;
			}
			if(options == null){
				options =  ImageLoader.this.bitmapOptions;
			}
			this.options = options;
			this.imageView = imageView;
			this.image = new LocalImage(context,localFile,size,options);
			this.loadListener = loadListener;
		}
		
		public WorkItem(ImageView imageView, HttpURLConnection conn,File diskFile,Size size,Options options,HttpLoaderListener loadListener) {
			if(size == null){
				size = ImageLoader.this.size;
			}
			if(diskFile == null && httpCacheDir != null){
				diskFile = new File(httpCacheDir,""+conn.getURL().toString().hashCode());
			}
			if(options == null){
				options =  ImageLoader.this.bitmapOptions;
			}
			this.imageView = imageView;
			this.options = options;
			this.image = new HttpImage(context, conn, diskFile, size, options);
			this.loadListener = loadListener;
			if(loadListener != null){
				((HttpImage)image).setDownloadListener(loadListener.getDownloadListener());
			}
		}
		
		

		private void run() throws OutOfMemoryError,IOException{
			
			//从磁盘缓存中获取图片
			if(!cancel){
				if(loadListener != null){
					loadListener.start();
				}
				Bitmap bitmap = imageCache.getBitmapFromDiskCache(image.toString(), size, options);
				if (bitmap == null) {
					//加载网络图片
					bitmap = image.getImage();
					
				}
				final Bitmap bm = bitmap;
				//图片木有被回收的时候才更新
				if (bitmap != null && !bm.isRecycled()) {
					uiRunnable = new Runnable() {

						public void run() {
							if(!cancel){
								imageView.setImageBitmap(bm);	
							}
						}
					};
					handler.post(uiRunnable);
					if(size == null){
						imageCache.addBitmapToMemoryCache(image.toString(), bm);
					}else{
						imageCache.addBitmapToCache(image.toString(), bm);
					}
					
				}
				if(loadListener != null){
					loadListener.end();
				}
			}
			

		}

		public void cancel() {
			
			cancel = true;
			//立即停止加载
			if (image != null) {
				image.cancel();
			}
			//从hanlder中删除ui更新
			if (uiRunnable != null) {
				handler.removeCallbacks(uiRunnable);
			}
			if(loadListener != null){
				loadListener.cancel();
			}
		}
		
		
		
		public Image getImage() {
			return image;
		}
		
		public ImageView getImageView() {
			return imageView;
		}

	}

	/**
	 * 构造器
	 * @param context
	 * @param threadCount 线程个数
	 */
	public ImageLoader(Context context, int threadCount) {
		this.context = context;
		imageCache = ImageCache.getInstance(context);
		threadPool = new WorkThread[threadCount];
		for(int i = 0;i<threadCount;i++ ){
			WorkThread thread = new WorkThread();
			threadPool[i] = thread;
			thread.start();
		}
	}

	private static interface WorkItemFactory{
		public WorkItem createWorkItem();
	}
	
	public synchronized void load(WorkItemFactory factory,Drawable loadBitmap){
		LogUtils.d(TAG, "workItemCount:"+queue.size());
		if(loadBitmap == null){
			loadBitmap = this.loadBitmap;
		}
		
		WorkItem newItem = factory.createWorkItem();
		ImageView imageView = newItem.getImageView();
		String key = newItem.getImage().toString();
		
		WorkItem item = map.get(imageView);
		//如果是对象复用的,则立即停止图片加载
		if(item != null){
			item.cancel();
		}
		
		//从内存中获取图片
		Bitmap bitmap = imageCache.getBitmapFromMemoryCache(key, size);
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
		} else {
			//设置图片加载提示
			imageView.setImageDrawable(loadBitmap);
			
			synchronized (queue) {
				queue.remove(item);
				map.put(imageView, newItem);
				queue.add(newItem);
				//新增任务,唤醒线程继续执行
				queue.notifyAll();
			}
		}
	}

	/**
	 * 
	 * @param path 加载
	 * @param imageView 
	 * @param loadBitmap 加载提示
	 */
	
	public void load(final ImageView imageView,final File localImage, final Size size,Drawable loadBitmap,final Options options,final LoadListener loadListener) {
		load(new WorkItemFactory() {
			
			public WorkItem createWorkItem() {
				// TODO Auto-generated method stub
				return new WorkItem(imageView, localImage, size, options,loadListener);
			}
		}, loadBitmap);
	}
	
	public void load(final ImageView imageView,final File localImage, final Size size,Drawable loadBitmap,final LoadListener loadListener) {
		load(imageView,localImage,size,loadBitmap,null,loadListener);
	}
	
	public void load(final ImageView imageView,final File localImage, final Size size,final LoadListener loadListener){
		load(imageView,localImage,size,null,null,loadListener);
	}
	
	public void load(final ImageView imageView,final File localImage,final LoadListener loadListener){
		load(imageView,localImage,null,null,null,loadListener);
	}
	
	public void load(final ImageView imageView,final File localImage){
		load(imageView,localImage,null,null,null,null);
	}
	
	public void load(final ImageView imageView,final HttpURLConnection conn,final Size size,final File saveFile,Drawable loadBitmap,final Options options,final HttpLoaderListener loaderListener){
		load(new WorkItemFactory() {
			
			public WorkItem createWorkItem() {
				// TODO Auto-generated method stub
				return new WorkItem(imageView, conn, saveFile, size, options,loaderListener);
			}
		},loadBitmap);
	}
	
	public void load(final ImageView imageView,final HttpURLConnection conn,final Size size,final File saveFile,Drawable loadBitmap,final HttpLoaderListener loaderListener){
		load(imageView,conn,size,saveFile,loadBitmap,null,loaderListener);
	}
	
	public void load(final ImageView imageView,final HttpURLConnection conn,final Size size,final File saveFile,final HttpLoaderListener loaderListener){
		load(imageView,conn,size,saveFile,null,null,loaderListener);
	}
	
	public void load(final ImageView imageView,final HttpURLConnection conn,final Size size,final HttpLoaderListener loaderListener){
		load(imageView,conn,size,null,null,null,loaderListener);
	}
	
	public void load(final ImageView imageView,final HttpURLConnection conn,final HttpLoaderListener loaderListener){
		load(imageView,conn,null,null,null,null,loaderListener);
	}
	public void load(final ImageView imageView,final HttpURLConnection conn){
		load(imageView,conn,null,null,null,null,null);
	}
	
	
	public void setHttpCacheDir(File httpCacheDir) {
		this.httpCacheDir = httpCacheDir;
	}


	/**
	 * setLoadBitmap
	 * @param loadBitmap
	 */
	public void setLoadBitmap(Drawable loadBitmap) {
		this.loadBitmap = loadBitmap;
	}

	/**
	 * setLoadBitmap
	 * @param resId
	 */
	public void setLoadBitmap(int resId) {
		this.loadBitmap = context.getResources().getDrawable(resId);
	}

	/**
	 * setImageSize 正方形
	 * @param size
	 */
	public void setImageSize(int size) {
		this.size = new Size(size, size);
	}
	
	public void setBitmapOptions(Options bitmapOptions) {
		this.bitmapOptions = bitmapOptions;
	}

	/**
	 * 
	 * @param width
	 * @param height
	 */
	public void setImageSize(int width, int height) {
		this.size = new Size(width, height);
	}
	
	public void setPause(boolean pause) {
		this.pause = pause;
		if(!pause){
			queue.notifyAll();
		}
	}
	public boolean isPause() {
		return pause;
	}
	
	/**
	 * 摧毁
	 */
	public void destory(){
		for(WorkThread thread : threadPool){
			thread.quit();
		}
		map.clear();
		synchronized (queue) {
			queue.clear();
		}
	}

}
