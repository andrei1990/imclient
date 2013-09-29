package com.im.utils;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.im.interfaces.IUpdateData;
import com.im.models.Debug;
import com.im.models.FriendModel;
import com.im.models.MessageModel;

public class XMLMessageParser extends DefaultHandler {
	
	private IUpdateData updater;
	
	public XMLMessageParser(IUpdateData updater) {
		super();
		this.updater = updater;
	}

	private Vector<MessageModel> messages = new Vector<MessageModel>();
	private Vector<Debug>  debugList=new Vector<Debug>();
	public void endDocument() throws SAXException 
	{
		this.updater.updateMessageListData(messages);
		this.updater.getDebugList(debugList);
		super.endDocument();
	}		
	
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException 
	{				
		if (localName == "message")
		{
			MessageModel message = new MessageModel();
			message.setImMessage(attributes.getValue(MessageModel.IM_MESSAGE));
			message.setSender(attributes.getValue(MessageModel.SENDER));
			messages.add(message);
		}
		else if (localName == "update") {
			Debug debug=new Debug();
			debug.setMessageUpdated(attributes.getValue("updated"));
			debugList.add(debug);
			
			
		}
		super.startElement(uri, localName, name, attributes);
	}

	public void startDocument() throws SAXException {			
		this.messages.clear();
		debugList.clear();
		
		super.startDocument();
	}
	
	
}
