package com.github.zimengle.imageloader;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

/**
 * 本地图片类,负责加载本地图片
 * @author zhangzimeng<zhangzimeng01@baidu.com>
 *
 */
public class LocalImage extends Image {
	
	private File file;

	private Size size;
	
	private Options options;
	
	private Context context;
	
	private boolean cancel = false;

	/**
	 * 构造方法
	 * @param context
	 * @param file 本地文件路径
	 * @param size 生成的尺寸
	 * @param options 图片选项
	 */
	public LocalImage(Context context,File file, Size size,Options options) {
		if(options == null){
			options = new Options();
		}
		this.context = context;
		this.file = file;
		this.size = size;
		this.options = options;
	}
	
	public LocalImage(Context context,File file, Size size) {
		this(context,file,size,null);
	}

	public LocalImage(Context context,File file){
		this(context,file,null,null);
	}
	
	@Override
	protected Bitmap generateImage() throws OutOfMemoryError {
		Bitmap bitmap = null;
		if(!cancel){
			if(size == null){
				options.inJustDecodeBounds = false;
				bitmap = BitmapFactory.decodeFile(file.toString(),options);
			}else{
				bitmap = BitmapUtils.getThumbnail(context, file.toString(), size.width, size.height,options );
			}
		}
		return bitmap;
	}

	/**
	 *  
	 */
	@Override
	public void cancel() {	
		
		cancel = true;
		
		options.requestCancelDecode();
		
		destory();

	}

	@Override
	public String toString() {
		return getLocalImage().toString();
	}
	
	public File getLocalImage(){
		return file;
	}

	

}
