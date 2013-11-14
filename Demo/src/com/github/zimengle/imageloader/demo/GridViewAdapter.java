package com.github.zimengle.imageloader.demo;

import java.util.List;


import com.github.zimengle.imageloader.ImageLoader;




import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class GridViewAdapter extends BaseAdapter{
	
	private List<String> list;
	
	private Context context;
	
	private ImageLoader imageLoader;
	
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

	public View getView(int position, View convertView, ViewGroup parent) {
		
		String path = list.get(position);
		
		
		
		if(convertView == null){
			convertView = LayoutInflater.from(context).inflate(R.layout.image, null);
		}
		
		imageLoader.setLoadBitmap(R.drawable.ic_launcher);
		
//		convertView.setTag(position);
		
		imageLoader.load(path, (ImageView)convertView);
		
		return convertView;
	}

}
