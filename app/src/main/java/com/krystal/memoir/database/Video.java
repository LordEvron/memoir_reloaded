package com.krystal.memoir.database;

public class Video {
	public int key;
	public String path;
	public String thumbnailPath;
	public long date;
	public boolean selected;
	public long length;
	public boolean userTaken;
	
	public Video(int k, long d, String p, String tp, boolean s, long l, boolean userTaken) {
		this.key = k;
		this.path = p;
		this.thumbnailPath = tp;
		this.date = d;
		this.selected = s;
		this.length = l;
		this.userTaken = userTaken;
	}

	public Video(int k, long d, String p, boolean s, long l, boolean userTaken) {
		this.key = k;
		this.path = p;
		this.date = d;
		this.selected = s;
		this.length = l;
		this.userTaken = userTaken;
	}
	
	public Video(String p) {
		this.path = p;
	}
}
