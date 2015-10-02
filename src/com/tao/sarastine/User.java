package com.tao.sarastine;

public class User{

	private String name;
	private String lastTalk;
	private String lastDate;
	private int iconId;

	public User(String name, int iconId){
		this.name = name;
		this.iconId = iconId;

	}

	public String getName(){
		return name;
	}

	public void setLastTalk(String lastTalk){
		this.lastTalk = lastTalk;
	}

	public String getLastTalk(){
		return lastTalk;
	}

	public void setLastDate(String lastDate){
		this.lastDate = lastDate;
	}

	public String getLastDate(){
		return lastDate;
	}

	public int getIcon(){
		return iconId;
	}
}
