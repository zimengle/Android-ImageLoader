package com.github.zimengle.imageloader.demo;

import java.util.ArrayList;
import java.util.List;




import android.app.Activity;
import android.os.Bundle;
import android.widget.BaseAdapter;
import android.widget.GridView;

public class MainActivity extends Activity{

	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		GridView gridView = (GridView) findViewById(R.id.gridview);
		List<String> list = new ArrayList<String>();
//		Cursor cursor = this.getContentResolver().query(Images.Media.EXTERNAL_CONTENT_URI, new String[]{ImageColumns.DATA}, null, null,null);
//		cursor.moveToFirst();
//	    do {
//	        list.add(cursor.getString(0));
//	    } while (cursor.moveToNext());
//	    
	    for(int i = 1;i<=634;i++){
	    	list.add("https://raw.github.com/zimengle/Static/master/Images/"+i+".jpg");
	    }
	    
		BaseAdapter adapter = new GridViewAdapter(this, list);
	    gridView.setAdapter(adapter);
	    adapter.notifyDataSetChanged();
	    
	}
	
	
	
}
