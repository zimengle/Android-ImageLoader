package com.github.zimengle.imageloader;



import java.io.IOException;

import android.graphics.Bitmap;

/**
 * 图片抽象类
 * @author zhangzimeng<zhangzimeng01@baidu.com>
 */
public abstract class Image {

	/**
	 * 带有长宽的尺寸类
	 * @author zhangzimeng<zhangzimeng01@baidu.com>
	 */
	public static class Size {
		
		public int width;
		public int height;

		public Size(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public Size(int size) {
			this.width = size;
			this.height = size;
		}

	}


	private Bitmap bitmap;

	public Image() {
	}

	/**
	 * 图片具有取消加载能力,请子类实现
	 */
	public abstract void cancel();

	/**
	 * 子类必须实现生成图片方法
	 * @return
	 * @throws OutOfMemoryError
	 * @throws IOException
	 */
	protected abstract Bitmap generateImage() throws OutOfMemoryError,IOException;

	/**
	 * 获取图片
	 * @return
	 * @throws OutOfMemoryError
	 * @throws IOException
	 */
	public Bitmap getImage() throws OutOfMemoryError, IOException {
		Bitmap bitmap = this.bitmap;
		if (bitmap == null || bitmap.isRecycled()) {

			bitmap = generateImage();
			if (bitmap != null) {
				this.bitmap = bitmap;
			}

		}

		return bitmap;
	}

	/**
	 * 回收图片
	 */
	public void destory() {
		if (bitmap != null && bitmap.isRecycled()) {
			bitmap.recycle();
		}
	}
	
	public abstract String toString();
	
	

}
