package com.github.zimengle.imageloader;

import java.io.IOException;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.media.ExifInterface;
import android.provider.MediaStore.Images.Media;

import android.text.TextUtils;

public class BitmapUtils {

	private static final int UNCONSTRAINED = -1;

	private static int computeInitialSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 : (int) Math
				.ceil(Math.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == UNCONSTRAINED) ? 128 : (int) Math
				.min(Math.floor(w / minSideLength),
						Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if ((maxNumOfPixels == UNCONSTRAINED)
				&& (minSideLength == UNCONSTRAINED)) {
			return 1;
		} else if (minSideLength == UNCONSTRAINED) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}

	private static int computeSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	/**
	 * 从图片EXIF从读取缩略图
	 * @param filePath
	 * @param targetSize
	 * @param maxPixels
	 * @return
	 */
	public static Bitmap createThumbnailFromEXIF(String filePath,
			int targetSize, int maxPixels) {
		if (filePath == null)
			return null;

		ExifInterface exif = null;
		byte[] thumbData = null;
		try {
			exif = new ExifInterface(filePath);
			thumbData = exif.getThumbnail();
		} catch (IOException ex) {
			return null;
		}

		BitmapFactory.Options fullOptions = new BitmapFactory.Options();
		BitmapFactory.Options exifOptions = new BitmapFactory.Options();
		int exifThumbWidth = 0;
		int fullThumbWidth = 0;

		// Compute exifThumbWidth.
		if (thumbData != null) {
			exifOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length,
					exifOptions);
			exifOptions.inSampleSize = computeSampleSize(exifOptions,
					targetSize, maxPixels);
			exifThumbWidth = exifOptions.outWidth / exifOptions.inSampleSize;
		}

		// Compute fullThumbWidth.
		fullOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, fullOptions);
		fullOptions.inSampleSize = computeSampleSize(fullOptions, targetSize,
				maxPixels);
		fullThumbWidth = fullOptions.outWidth / fullOptions.inSampleSize;

		// Choose the larger thumbnail as the returning sizedThumbBitmap.
		if (thumbData != null && exifThumbWidth >= fullThumbWidth) {
			// int width = exifOptions.outWidth;
			// int height = exifOptions.outHeight;
			exifOptions.inJustDecodeBounds = false;
			Bitmap bitmap = BitmapFactory.decodeByteArray(thumbData, 0,
					thumbData.length, exifOptions);
			return bitmap;
		} else {
			fullOptions.inJustDecodeBounds = false;
			Bitmap bitmap = BitmapFactory.decodeFile(filePath, fullOptions);
			return bitmap;
		}
	}

	/**
	 * 从数据库中查询图片方向
	 * @param context
	 * @param path
	 * @return
	 */
	public static int getOrientation(Context context, String path) {
		int ret = 0;
		if (TextUtils.isEmpty(path)) {
			return ret;
		}
		ContentResolver resolver = context.getContentResolver();
		String[] projection = new String[] { Media.ORIENTATION, Media.DATA };
		String selection = Media.DATA + " =? ";
		String[] selectionArgs = new String[] { path };
		Cursor cursor = resolver.query(Media.EXTERNAL_CONTENT_URI, projection,
				selection, selectionArgs, null);
		if (cursor == null) {
			return -1;
		}
		if (cursor.getCount() == 0) {
			cursor.close();
			return -1;
		} else {
			cursor.moveToFirst();
			int orienation = cursor.getInt(0);
			cursor.close();
			return orienation;
		}
	}

	/**
	 * 获取缩略图
	 * @param context
	 * @param imagePath
	 * @param destWidth
	 * @param destHeight
	 * @param opts
	 * @return
	 */
	public static Bitmap getThumbnail(Context context, String imagePath,
			int destWidth, int destHeight, Options opts) {

		try {
			Bitmap source = createThumbnailFromEXIF(imagePath, destWidth,
					UNCONSTRAINED);
			if (source == null) {
				final int MAX_SIZE = destWidth * destHeight;
				if (opts == null) {
					opts = new Options();
				}

				opts.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(imagePath, opts);
				int width = opts.outWidth;
				int height = opts.outHeight;
				int scale = 1;

				while ((width * height) * (1 / Math.pow(scale, 2)) > MAX_SIZE) {
					scale++;
				}

				opts.inSampleSize = scale;
				opts.inScaled = true;
				opts.inJustDecodeBounds = false;
				opts.inPurgeable = true;
				opts.inInputShareable = true;
				if (!opts.mCancel) {
					source = BitmapFactory.decodeFile(imagePath, opts);
				}
			}

			if (source != null) {
				Matrix m = new Matrix();
				int orientation = getOrientation(context, imagePath);
				if (orientation != -1)
					m.postRotate(orientation);
				int size = Math.min(source.getWidth(), source.getHeight());
				int x = 0, y = 0;
				if (size < source.getWidth()) {
					x = (source.getWidth() - size) / 2;
				}
				if (size < source.getHeight()) {
					y = (source.getHeight() - size) / 2;
				}
				Bitmap tmp = Bitmap.createBitmap(source, x, y, size, size, m,
						true);
				if (tmp != source) {
					source.recycle();
				}
				Bitmap bitmap = Bitmap.createBitmap(destWidth, destHeight,
						Config.RGB_565);
				Canvas canvas = new Canvas(bitmap);
				Rect src = new Rect(0, 0, tmp.getWidth(), tmp.getHeight());
				Rect dst = new Rect(0, 0, destWidth, destHeight);
				Paint paint = new Paint();
				canvas.drawBitmap(tmp, src, dst, paint);
				tmp.recycle();
				return bitmap;
			} else {
				return null;
			}
		} catch (OutOfMemoryError e) {
			// TODO: handle exception
			e.printStackTrace();

			return null;
		}
	}
}
