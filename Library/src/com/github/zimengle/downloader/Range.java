package com.github.zimengle.downloader;

public class Range {
	private Long startPos;
	private Long endPos;
	public Range(long startPos, long endPos) {
		this.startPos = startPos;
		this.endPos = endPos;
	}
	public Range(long startPos) {
		this.startPos = startPos;
	}
	public String getRangeHeader(){
		String rangeStr = "bytes="+startPos+"-";
		if(endPos != null){
			rangeStr += endPos;
		}
		return rangeStr;
	}
}
