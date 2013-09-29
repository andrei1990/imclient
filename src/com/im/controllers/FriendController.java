package com.im.controllers;

import com.im.models.FriendModel;


/*
 * This class can store friendInfo and check userkey and username combination 
 * according to its stored data
 */
public class FriendController 
{
	
	private static FriendModel[] friendsInfo = null;
	private static FriendModel[] unapprovedFriendsInfo = null;
	private static String activeFriend;
	
	public static void setFriendsInfo(FriendModel[] friendInfo)
	{
		FriendController.friendsInfo = friendInfo;
	}
	
	
	
	public static FriendModel checkFriend(String username, String userKey)
	{
		FriendModel result = null;
		if (friendsInfo != null) 
		{
			for (int i = 0; i < friendsInfo.length; i++) 
			{
				if ( friendsInfo[i].userName.equals(username) && 
					 friendsInfo[i].userKey.equals(userKey)
					)
				{
					result = friendsInfo[i];
					break;
				}				
			}			
		}		
		return result;
	}
	
	public static void setActiveFriend(String friendName){
		activeFriend = friendName;
	}
	
	public static String getActiveFriend()
	{
		return activeFriend;
	}



	public static FriendModel getFriendInfo(String username) 
	{
		FriendModel result = null;
		if (friendsInfo != null) 
		{
			for (int i = 0; i < friendsInfo.length; i++) 
			{
				if ( friendsInfo[i].userName.equals(username) )
				{
					result = friendsInfo[i];
					break;
				}				
			}			
		}		
		return result;
	}



	public static void setUnapprovedFriendsInfo(FriendModel[] unapprovedFriends) {
		unapprovedFriendsInfo = unapprovedFriends;		
	}



	public static FriendModel[] getFriendsInfo() {
		return friendsInfo;
	}



	public static FriendModel[] getUnapprovedFriendsInfo() {
		return unapprovedFriendsInfo;
	}
	
	
	

}
