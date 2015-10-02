package com.tao.sarastine;

public class Utt{

	private String utt;
	private boolean isMe;
	private String date;

	public Utt(String utt, boolean isMe, String date){
		this.utt = utt;
		this.isMe = isMe;
		this.date = date;
	}

	public String getUtt(){
		return utt;
	}

	public boolean getIsMe(){
		return isMe;
	}

	public String getDate(){
		return date;
	}
}
