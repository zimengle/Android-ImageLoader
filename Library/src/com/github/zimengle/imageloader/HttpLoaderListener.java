package com.github.zimengle.imageloader;

import com.github.zimengle.downloader.DownloadListener;

public interface HttpLoaderListener extends LoadListener{

	public DownloadListener getDownloadListener();
	
}
