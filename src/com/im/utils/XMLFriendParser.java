package com.im.utils;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.im.interfaces.IUpdateData;
import com.im.models.FriendModel;



public class XMLFriendParser extends DefaultHandler {
	private String userKey = new String();
	private IUpdateData updater;

	public XMLFriendParser(IUpdateData updater) {
		super();
		this.updater = updater;
	}

	private Vector<FriendModel> mFriends = new Vector<FriendModel>();
	private Vector<FriendModel> mOnlineFriends = new Vector<FriendModel>();
	private Vector<FriendModel> mUnapprovedFriends = new Vector<FriendModel>();

	public void endDocument() throws SAXException {
		FriendModel[] friends = new FriendModel[mFriends.size()
				+ mOnlineFriends.size()];

		int onlineFriendCount = mOnlineFriends.size();
		for (int i = 0; i < onlineFriendCount; i++) {
			friends[i] = mOnlineFriends.get(i);
		}

		int offlineFriendCount = mFriends.size();
		for (int i = 0; i < offlineFriendCount; i++) {
			friends[i + onlineFriendCount] = mFriends.get(i);
		}

		int unApprovedFriendCount = mUnapprovedFriends.size();
		FriendModel[] unApprovedFriends = new FriendModel[unApprovedFriendCount];

		for (int i = 0; i < unApprovedFriends.length; i++) {
			unApprovedFriends[i] = mUnapprovedFriends.get(i);
		}

		this.updater.updateData(friends, unApprovedFriends, userKey);

		super.endDocument();
	}

	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		if (localName == "friend") {
			FriendModel friend = new FriendModel();
			friend.userName = attributes.getValue(FriendModel.USERNAME);
			String status = attributes.getValue(FriendModel.STATUS);
			friend.ip = attributes.getValue(FriendModel.IP);
			friend.port = attributes.getValue(FriendModel.PORT);
			friend.userKey = attributes.getValue(FriendModel.USER_KEY);
			

			if (status != null && status.equals("online")) {
				friend.status = STATUS.ONLINE;
				mOnlineFriends.add(friend);
			} else if (status.equals("unApproved")) {
				friend.status = STATUS.UNAPPROVED;
				mUnapprovedFriends.add(friend);
			} else {
				friend.status = STATUS.OFFLINE;
				mFriends.add(friend);
			}
		} else if (localName == "user") {
			this.userKey = attributes.getValue(FriendModel.USER_KEY);
		}
		super.startElement(uri, localName, name, attributes);
	}

	@Override
	public void startDocument() throws SAXException {
		this.mFriends.clear();
		this.mOnlineFriends.clear();
		super.startDocument();
	}

}
