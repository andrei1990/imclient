package com.im.interfaces;
import java.util.ArrayList;
import java.util.Vector;

import com.im.models.Debug;
import com.im.models.FriendModel;
import com.im.models.MessageModel;


public interface IUpdateData {
	public void updateData(FriendModel[] friends, FriendModel[] unApprovedFriends, String userKey);
	public void updateMessageListData(Vector<MessageModel> messages);
	public void getDebugList(Vector<Debug> debugList);
}
