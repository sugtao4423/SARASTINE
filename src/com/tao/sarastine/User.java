package com.tao.sarastine;

public class User {
	
	private String name, lastTalk, lastDate;
	private int iconId;
	
	public void setName(String name){
		this.name = name;
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
	public void setIcon(int iconId){
		this.iconId = iconId;
	}
	public int getIcon(){
		return iconId;
	}
}
