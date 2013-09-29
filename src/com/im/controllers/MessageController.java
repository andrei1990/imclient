package com.im.controllers;

import java.util.ArrayList;
import java.util.Vector;

import com.im.models.MessageModel;

public class MessageController {
	private static Vector<MessageModel> messageModels;
	
	public static void setMessages(Vector<MessageModel> messageModel){
		messageModels=messageModel;
	}
	
	public static Vector<MessageModel> getMessages(){
		return messageModels;
	}

}
