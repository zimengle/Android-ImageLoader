package com.github.zimengle.downloader;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 
 * 唯一URL下载,保证所有下载只有一个URL下载正在执行,同一URL采取排队策略
 * @author zhangzimeng<zhangzimeng01@baidu.com>
 *
 */
public class UniqueURLDownloader implements Downloader {

	//一个url对应一个下载器队列
	private static Map<String, Downloader> map = new HashMap<String, Downloader>();

	private Downloader downloader;

	public UniqueURLDownloader(Downloader downloader) {
		this.downloader = downloader;
	}

	public synchronized boolean download() throws IOException {
		
		synchronized (map) {
			Downloader prevDownloader = map.get(getURL());
			if(prevDownloader != null){
				return false;
			}
			map.put(getURL(), downloader);
		}
		boolean result = false;
		try{
			result = downloader.download();
		}finally{
			synchronized (map) {
				map.remove(getURL());
			}
		}
		
		return result;
	}

	public void cancel() {
		downloader.cancel();
	}

	public String getURL() {

		return downloader.getURL();
	}

}
