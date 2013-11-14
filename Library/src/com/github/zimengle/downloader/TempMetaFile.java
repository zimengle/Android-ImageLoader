package com.github.zimengle.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.Map;

/**
 * 下载文件的元数据记录,通过对象流方式保存到文件中,以便在断点下载过程中校验文件的完整性
 * @author zhangzimeng<zhangzimeng01@baidu.com>
 *
 */
public class TempMetaFile {
	
	/**
	 * 元数据,包含etag,lastmodify,contentlength
	 * @author zhangzimeng<zhangzimeng01@baidu.com>
	 */
	public static class Meta implements Serializable{
		private static final long serialVersionUID = -7984029156207800687L;
		public String etag;
		public Long lastModify ;
		public Integer contentLength ;
		
		public Meta() {
			// TODO Auto-generated constructor stub
		}
		
		public Meta(String etag, Long lastModify, Integer contentLength) {
			this.etag = etag;
			this.lastModify = lastModify;
			this.contentLength = contentLength;
		}
		/**
		 * 首先判断etag,如果etag一致,说明文件是一致的
		 * 然后将lastmodify和contentlength一起判断
		 * @param meta
		 * @return
		 */
		public boolean check(Meta meta){
			if(etag != null && etag.equals(meta.etag)){
				return true;
			}
			if(lastModify != null && meta.lastModify == lastModify && contentLength != null && meta.contentLength == contentLength){
				return true;
			}
			return false;
		}
		
	}
	
	private static TempMetaFile instance = null;
	/**
	 * 单例模式
	 * @param meteFile
	 * @return
	 */
	public static TempMetaFile getInstance(File meteFile){
		if(instance == null){
			synchronized (TempMetaFile.class) {
				if(instance == null){
					instance = new TempMetaFile(meteFile);
				}
			}
		}
		return instance;
	}
	
	private File metaFile;
	
	private Map<String, Meta> map;
	
	private TempMetaFile(File metaFile) {
		this.metaFile = metaFile;
		init();
	}
	
	@SuppressWarnings("unchecked")
	private void init() {
		File dir = metaFile.getParentFile();
		if(!dir.exists()){
			dir.mkdirs();
		}
		if(!metaFile.exists()){
			try {
				metaFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ObjectInputStream input = null;
		try {
			 input = new ObjectInputStream(new FileInputStream(metaFile));
			 map = (Map<String, Meta>) input.readObject();
		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(input != null){
				try {
					input.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if(map == null){
			map = new HashMap<String, TempMetaFile.Meta>();
		}
		
	}
	
	/**
	 * put
	 * @param key
	 * @param meta
	 */
	public synchronized void put(String key,Meta meta){
		map.put(key, meta);
		save();
	}
	
	/**
	 * get
	 * @param key
	 * @return
	 */
	public synchronized Meta get(String key){
		return map.get(key);
	}
	
	/**
	 * 清空
	 */
	public void clear(){
		map.clear();
		save();
	}
	
	/**
	 * 删除key
	 * @param key
	 */
	public synchronized void remove(String key){
		map.remove(key);
		save();
	}
	
	/**
	 * 删除元数据文件
	 */
	public void delete(){
		metaFile.delete();
	}
	
	/**
	 *  保存
	 */
	private void save(){
		ObjectOutputStream out = null;
		try {
			 out = new ObjectOutputStream(new FileOutputStream(metaFile));
			 out.writeObject(map);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	

	
	
	
	
	
	
	
	
}
