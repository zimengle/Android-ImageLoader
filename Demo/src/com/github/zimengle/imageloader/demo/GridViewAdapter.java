package com.github.zimengle.imageloader.demo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;


import com.github.zimengle.downloader.DownloadListener;
import com.github.zimengle.imageloader.HttpImage;
import com.github.zimengle.imageloader.HttpLoaderListener;
import com.github.zimengle.imageloader.ImageLoader;
import com.github.zimengle.imageloader.LogUtils;




import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class GridViewAdapter extends BaseAdapter{
	
	private static final String TAG = "GridViewAdapter";
	
	private List<String> list;
	
	private Context context;
	
	private ImageLoader imageLoader;
	
	private Handler handler = new Handler();
	
	
	public GridViewAdapter(Context context,List<String> list) {
		this.context = context;
		this.list = list;
		imageLoader = new ImageLoader(context, 5);
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		imageLoader.setImageSize(wm.getDefaultDisplay().getWidth()/3);
	}

	public int getCount() {
		return list.size();
	}

	public Object getItem(int position) {
		return list.get(position);
	}

	public long getItemId(int position) {
		return position;
	}
	
	private static class ViewHolder{
		public ImageView imageView;
		public ProgressBar bar;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		
		final String path = list.get(position);
		
		
		ViewHolder holder = null;
		if(convertView == null){
			convertView = LayoutInflater.from(context).inflate(R.layout.image, null);
			holder = new ViewHolder();
			holder.imageView = (ImageView) convertView.findViewById(R.id.img);
			holder.bar = (ProgressBar) convertView.findViewById(R.id.progress);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		final ImageView imageView = holder.imageView;
		
		final ProgressBar progressBar = holder.bar;
		
		progressBar.setVisibility(View.GONE);
		
//		imageLoader.setLoadBitmap(R.drawable.ic_launcher);

		try {
			HttpURLConnection conn = (HttpURLConnection)new URL(path).openConnection();
			imageLoader.load((ImageView)imageView,conn,new HttpLoaderListener() {
				
				public void start() {
					handler.post(new Runnable() {
						
						public void run() {
							progressBar.setVisibility(View.VISIBLE);
							progressBar.setProgress(0);
							
						}
					});
					LogUtils.d(TAG, "load_start:"+path);
					
				}
				
				public void end() {
					handler.post(new Runnable() {
						
						public void run() {
							progressBar.setVisibility(View.GONE);
							
						}
					});
					LogUtils.d(TAG, "load_end:"+path);
					
				}
				
				public void cancel() {
					handler.post(new Runnable() {
						
						public void run() {
							progressBar.setVisibility(View.GONE);
							
						}
					});
					LogUtils.d(TAG, "load_cancel:"+path);
					
				}
				
				public DownloadListener getDownloadListener() {
					// TODO Auto-generated method stub
					return new DownloadListener(){

						public void start(HttpURLConnection connection) {
							
//							
							LogUtils.d(TAG, "http_load_start:"+path);
							
						}

						public void transfer(final long loaded, final long total,
								HttpURLConnection connection) {
							handler.post(new Runnable() {
								
								public void run() {
									progressBar.setProgress((int)(loaded/total));
									
								}
							});
							
							LogUtils.d(TAG,"url:"+path+"\nloaded:"+loaded+"\ntotal:"+total);
							
						}

						public void success(HttpURLConnection connection) {
							LogUtils.d(TAG, "http_load_success:"+path);
							
						}

						public void cancel(HttpURLConnection connection) {
							
							LogUtils.d(TAG, "http_load_cancel:"+path);
							
						}
						
					};
				}
			});
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return convertView;
	}

}
