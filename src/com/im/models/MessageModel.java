package com.im.models;

public class MessageModel {
	private String imMessage;
	private String sender;
	public static final String IM_MESSAGE="im_message";
	public static final String SENDER="sender";

	public String getImMessage() {
		return imMessage;
	}

	public void setImMessage(String imMessage) {
		this.imMessage = imMessage;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}

	

}
