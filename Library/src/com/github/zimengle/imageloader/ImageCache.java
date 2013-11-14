package com.github.zimengle.imageloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.github.zimengle.imageloader.Image.Dimen;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.util.LruCache;

/**
 * 图片缓存器
 * @author zhangzimeng<zhangzimeng01@baidu.com>
 *
 */
public class ImageCache {
	
	private static final String TAG = "ImageCache";

	private static ImageCache instance;
	
	/**
	 * 单例获取实例
	 * @param context
	 * @param params
	 * @return
	 */
	public static ImageCache getInstance(Context context,CacheParams params){
		if(instance == null){
			synchronized (ImageCache.class) {
				if(instance == null){
					instance = new ImageCache(context,params);
				}
			}
		}
		return instance;
	}
	
	/**
	 * 默认缓存参数获取实例
	 * @param context
	 * @return
	 */
	public static ImageCache getInstance(Context context){
		CacheParams cacheParams = new CacheParams();
		return getInstance(context,cacheParams);
	}
	
	/**
	 * 图片缓存参数
	 * @author zhangzimeng<zhangzimeng01@baidu.com>
	 */
	public static class CacheParams{
		/**
		 * 图片缓存
		 */
		public int cacheSize = 20*1024*1024;
		
		/**
		 * 磁盘缓存
		 */
		public File diskDir;
		
		/**
		 * 压缩格式
		 */
		public CompressFormat format = CompressFormat.JPEG;
		
		/**
		 * 压缩质量
		 */
		public int quality = 90;
		
	}
	
	private Context context;
	
	private CacheParams cacheParams;
	
	//Lru缓存器
	private LruCache<String, Bitmap> memoryCache;
	
	private ImageCache(Context context,CacheParams cacheParams) {
		this.cacheParams = cacheParams;
		this.context = context;
		init();
	}
	
	
	
	private void init() {
		//如果未指定缓存位置,则使用系统默认路径
		if(cacheParams.diskDir == null){
			cacheParams.diskDir = getDiskCacheDir(context, "imageloader");	
			LogUtils.d(TAG, "cacheDir"+cacheParams.diskDir);
		}
		if(!cacheParams.diskDir.exists()){
			cacheParams.diskDir.mkdirs();
		}
		memoryCache = new LruCache<String, Bitmap>(cacheParams.cacheSize){
			@Override
			protected int sizeOf(String key, Bitmap value) {
				// TODO Auto-generated method stub
				return Util.getBitmapSize(value);
			}
		};
		
	}

	/***
	 * 添加缓存,包括内存缓存和磁盘缓存
	 * @param key 
	 * @param bitmap
	 */
	public void addBitmapFromCache(String key,final Bitmap bitmap){
		if(bitmap != null && !bitmap.isRecycled()){
			if(memoryCache.get(key) == null){
				memoryCache.put(key, bitmap);
			}
			final File file = getDiskCacheFile(key,new Dimen(bitmap.getWidth(), bitmap.getHeight()));
			if(!file.exists()){
				save(file, bitmap);
			}
		}
	}
	
	/**
	 * 获取内存缓存
	 * @param key
	 * @param size
	 * @return
	 */
	public Bitmap getBitmapFromMemoryCache(String key,Dimen size){
		return memoryCache.get(key);
	}
	
	/**
	 * 从磁盘中获取缓存图片
	 * @param key
	 * @param dimen
	 * @param bitmapOptions
	 * @return
	 */
	public Bitmap getBitmapFromDiskCache(String key,Dimen dimen,Options bitmapOptions){
		Bitmap bitmap = null;
		File diskCacheFile = getDiskCacheFile(key,dimen);
		if(diskCacheFile.exists()){
			if(bitmapOptions == null){
				 bitmapOptions = new BitmapFactory.Options();
			}
			bitmapOptions.inJustDecodeBounds = false;
			bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565; 
			bitmapOptions.inPurgeable = true;
			bitmapOptions.inInputShareable = true;
			return BitmapFactory.decodeFile(diskCacheFile.toString(),bitmapOptions);
		}
		return bitmap;
	}
	
	
	/**
	 * 获取文件名,不同的尺寸图片会不一样
	 * @param name
	 * @param size
	 * @return
	 */
	private String getFileName(String name,Dimen size){
		String filename = name;
		if(size != null){
			filename += "_"+size.width+"*"+size.height;
		}
		return ""+filename.hashCode();
	}
	
	/**
	 * 获取磁盘缓存文件路径
	 * @param key
	 * @param size
	 * @return
	 */
	private File getDiskCacheFile(String key,Dimen size){
		
		return new File(cacheParams.diskDir,getFileName(key,size)+"."+cacheParams.format);
	}
	
	
	
	private void save(File file,Bitmap bitmap){
		try {
			if(!file.exists()){
				file.createNewFile();
			}
			bitmap.compress(cacheParams.format, cacheParams.quality, new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static File getDiskCacheDir(Context context, String uniqueName) {
//        final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !isExternalStorageRemovable() ? getExternalCacheDir(context).getPath() : context.getCacheDir().getPath();

        return new File(getExternalCacheDir(context).getPath() + File.separator + uniqueName);
	}
	
	@TargetApi(9)
    public static boolean isExternalStorageRemovable() {
        if (Util.hasGingerbread()) {
                return Environment.isExternalStorageRemovable();
        }
        return true;
    }
	
	@TargetApi(8)
    public static File getExternalCacheDir(Context context) {
       

        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
	}
	
	public CacheParams getCacheParams() {
		return cacheParams;
	}
	
	
}
