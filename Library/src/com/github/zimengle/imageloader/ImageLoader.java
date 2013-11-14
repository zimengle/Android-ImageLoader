package com.github.zimengle.imageloader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import com.github.zimengle.imageloader.Image.Dimen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
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
	private Dimen size;

	private Context context;
	
	private boolean pause;
	
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

		private String path;

		private Image image;

		private Runnable uiRunnable;
		
		private boolean cancel = false;
		
		//主动生成Options对象,在decode时候主动取消decode
		private Options bitmapOptions;

		public WorkItem(ImageView imageView, String path) {
			this.imageView = imageView;
			this.path = path;
		}

		public void run() throws OutOfMemoryError,IOException{
			bitmapOptions = new Options();
			//从磁盘缓存中获取图片
			Bitmap bitmap = imageCache.getBitmapFromDiskCache(path, size, bitmapOptions);
			if (bitmap == null) {
				//加载网络图片
				if (path.startsWith("http://") || path.startsWith("https://")) {
					try {
						image = new HttpImage(context, new File(imageCache.getCacheParams().diskDir+"/http",""+path.hashCode()), (HttpURLConnection)new URL(path).openConnection(), size,bitmapOptions);
					} catch (MalformedURLException e) {
					} catch (IOException e) {
						
					}
				} else {
					//加载本地图片
					image = new LocalImage(context,new File(path), size,bitmapOptions);
				}
				bitmap = image.getImage();
			}
			final Bitmap bm = bitmap;
			//图片木有被回收的时候才更新,不然会报错
			if (bitmap != null && !bm.isRecycled()) {
				uiRunnable = new Runnable() {

					public void run() {
						if(!cancel){
							imageView.setImageBitmap(bm);	
						}
					}
				};
				handler.post(uiRunnable);
				imageCache.addBitmapFromCache(path, bm);
			}

		}

		public void cancel() {
			
			cancel = true;
			//立即停止加载
			if (image != null) {
				image.cancel();
			}
			//如果正在decode,立即停止decode
			if(bitmapOptions != null){
				bitmapOptions.requestCancelDecode();
			}
			//从hanlder中删除ui更新
			if (uiRunnable != null) {
				handler.removeCallbacks(uiRunnable);
			}
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

	/**
	 * 
	 * @param path 如果path是http协议,加载网络图片,如果不是,则加载本地图片
	 * @param imageView 
	 * @param loadBitmap 加载提示
	 */
	
	public synchronized void load(String path, ImageView imageView, Drawable loadBitmap) {
		LogUtils.d(TAG, "workItemCount:"+queue.size());
		WorkItem item = map.get(imageView);
		//如果是对象复用的,则立即停止图片加载
		if(item != null){
			item.cancel();
		}
		//从内存中获取图片
		Bitmap bitmap = imageCache.getBitmapFromMemoryCache(path, size);
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
		} else {
			//设置图片加载提示
			if (loadBitmap != null) {
				imageView.setImageDrawable(loadBitmap);
			} else {
				imageView.setImageDrawable(this.loadBitmap);
			}
			
			synchronized (queue) {
				queue.remove(item);
				item = new WorkItem(imageView, path);
				map.put(imageView, item);
				queue.add(item);
				//新增任务,唤醒线程继续执行
				queue.notifyAll();
			}
			
			
			
			
		}

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
	
	/**
	 * load
	 * @param path
	 * @param imageView
	 * @param bitmap
	 */
	public void load(String path, ImageView imageView, Bitmap bitmap) {
		load(path, imageView, new BitmapDrawable(context.getResources(),bitmap));
	}

	/**
	 * load
	 * @param path
	 * @param imageView
	 * @param resId
	 */
	public void load(String path, ImageView imageView, int resId) {
		load(path, imageView, context.getResources().getDrawable(resId));
	}

	/**
	 * load
	 * @param path
	 * @param imageView
	 */
	public void load(String path, ImageView imageView) {
		load(path, imageView, (Drawable) null);
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
		this.size = new Dimen(size, size);
	}

	/**
	 * 
	 * @param width
	 * @param height
	 */
	public void setImageSize(int width, int height) {
		this.size = new Dimen(width, height);
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

}
