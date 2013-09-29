/* 
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.im.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.java.otr4j.OtrEngineImpl;

import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.im.communication.CommunicationManager;
import com.im.controllers.FriendController;
import com.im.controllers.MessageController;
import com.im.interfaces.IAppManager;
import com.im.interfaces.ICommunicationManager;
import com.im.interfaces.IUpdateData;
import com.im.models.Debug;
import com.im.models.FriendModel;
import com.im.models.MessageModel;
import com.im.ui.Login;
import com.im.ui.Messaging;
import com.im.utils.*;

/**
 * This is an example of implementing an application service that runs locally
 * in the same process as the application. The {@link LocalServiceController}
 * and {@link LocalServiceBinding} classes show how to interact with the
 * service.
 * 
 * <p>
 * Notice the use of the {@link NotificationManager} when interesting things
 * happen in the service. This is generally how background services should
 * interact with the user, rather than doing something more disruptive such as
 * calling startActivity().
 */
public class IMService extends Service implements IAppManager, IUpdateData {
	// private NotificationManager mNM;

	public static final String TAKE_MESSAGE = "Take_Message";
	public static final String FRIEND_LIST_UPDATED = "Take Friend List";
	public static final String UPDATE_MESSAGE_HISTORY = "Update Message";
	public ConnectivityManager conManager = null;
	private final int UPDATE_TIME_PERIOD = 1500;

	private String rawFriendList = new String();
	private String message = new String();

	ICommunicationManager socketOperator = new CommunicationManager(this);

	private final IBinder mBinder = new IMBinder();
	private String username;
	private String password;
	private String userKey;
	private boolean authenticatedUser = false;
	private OtrEngineImpl hostOtrEngineIml;
	// timer to take the updated data from server
	private Timer timer;

	private NotificationManager mNM;

	public class IMBinder extends Binder {
		public IAppManager getService() {
			return IMService.this;
		}

	}

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting. We put an icon in the
		// status bar.
		// showNotification();
		conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

		// Timer is used to take the friendList info every UPDATE_TIME_PERIOD;
		// timer = new Timer();

		Thread thread = new Thread() {
			@Override
			public void run() {

				// socketOperator.startListening(LISTENING_PORT_NO);
				Random random = new Random();
				int tryCount = 0;
				while (socketOperator.startListening(10000 + random
						.nextInt(20000)) == 0) {
					tryCount++;
					if (tryCount > 10) {
						// if it can't listen a port after trying 10 times, give
						// up...
						break;
					}

				}
			}
		};
		thread.start();

	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * Show a notification while this service is running.
	 * 
	 * @param msg
	 **/
	private void showNotification(String username, String msg) {
		// Set the icon, scrolling text and timestamp
		String title = username + ": "
				+ ((msg.length() < 5) ? msg : msg.substring(0, 5) + "...");
		Notification notification = new Notification(
				com.im.R.drawable.stat_sample, title,
				System.currentTimeMillis());

		Intent i = new Intent(this, Messaging.class);
		i.putExtra(FriendModel.USERNAME, username);
		i.putExtra(FriendModel.MESSAGE, msg);

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

		// Set the info for the views that show in the notification panel.
		// msg.length()>15 ? msg : msg.substring(0, 15);
		notification.setLatestEventInfo(this, "New message from " + username,
				msg, contentIntent);

		// TODO: it can be improved, for instance message coming from same user
		// may be concatenated
		// next version

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.
		mNM.notify((username + msg).hashCode(), notification);
	}

	public String getUsername() {
		return username;
	}

	public boolean sendMessage(String username, String message) {
		FriendModel friendInfo = FriendController.getFriendInfo(username);
		String IP = friendInfo.ip;
		// String IP = "10.0.2.2";
		int port = Integer.parseInt(friendInfo.port);

		String msg = FriendModel.USERNAME + "="
				+ URLEncoder.encode(this.username) + "&" + FriendModel.USER_KEY
				+ "=" + URLEncoder.encode(userKey) + "&" + FriendModel.MESSAGE
				+ "=" + URLEncoder.encode(message) + "&";

		return socketOperator.sendMessage(msg, IP, port);
	}

	public String sendMessageForUser(String sender, String receiver,
			String message) {
		String result = socketOperator.sendHttpRequest(concatMessageParams(
				sender, receiver, message));
		Log.i("message", result);
		return result;

	}

	private String getFriendList() {
		// after authentication, server replies with friendList xml

		rawFriendList = socketOperator
				.sendHttpRequest(getAuthenticateUserParams(username, password));
		if (rawFriendList != null) {
			this.parseFriendInfo(rawFriendList);
		}
		return rawFriendList;
	}

	/**
	 * authenticateUser: it authenticates the user and if succesful it returns
	 * the friend list or if authentication is failed it returns the "0" in
	 * string type
	 * */
	public String authenticateUser(String usernameText, String passwordText) {
		this.username = usernameText;
		this.password = passwordText;
		timer = new Timer();
		this.authenticatedUser = false;

		String result = this.getFriendList(); // socketOperator.sendHttpRequest(getAuthenticateUserParams(username,
												// password));
		if (result != null && !result.equals(Login.AUTHENTICATION_FAILED)) {
			Log.i("httpresultaut", result);
			// if user is authenticated then return string from server is not
			// equal to AUTHENTICATION_FAILED
			this.authenticatedUser = true;
			rawFriendList = result;

			Intent i = new Intent(FRIEND_LIST_UPDATED);
			i.putExtra(FriendModel.FRIEND_LIST, rawFriendList);
			sendBroadcast(i);

			timer.schedule(new TimerTask() {
				public void run() {
					try {
						// rawFriendList = IMService.this.getFriendList();
						// sending friend list
						Intent i = new Intent(FRIEND_LIST_UPDATED);
						Intent updateMessagingI = new Intent(
								UPDATE_MESSAGE_HISTORY);
						String tmp = IMService.this.getFriendList();
						String message = IMService.this
								.getMessageForReceiver(username);
						if (tmp != null) {
							i.putExtra(FriendModel.FRIEND_LIST, tmp);

							sendBroadcast(i);
							Log.i("friend list broadcast sent ", "");
						} else {
							Log.i("friend list returned null", "");
						}

						if (message != null) {
							updateMessagingI.putExtra(
									Messaging.GET_NEW_MESSAGE, message);
							sendBroadcast(updateMessagingI);
							Log.i("message list broadcast sent ", message);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, UPDATE_TIME_PERIOD, UPDATE_TIME_PERIOD);
		}

		return result;
	}

	private String getMessageForReceiver(String username) {
		message = socketOperator
				.sendHttpRequest(concatMessageRequestParams(username));
		if (message != null) {
			this.parseMessageXmlInfo(message);
		}
		return message;
	}

	private void parseMessageXmlInfo(String messageXmlResult) {
		// TODO Auto-generated method stub
		try {
			Log.i("messagexmlresult", messageXmlResult);
			SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
			sp.parse(new ByteArrayInputStream(messageXmlResult.getBytes()),
					new XMLMessageParser(IMService.this));
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void messageReceived(String message) {
		String[] params = message.split("&");
		String username = new String();
		String userKey = new String();
		String msg = new String();
		for (int i = 0; i < params.length; i++) {
			String[] localpar = params[i].split("=");
			if (localpar[0].equals(FriendModel.USERNAME)) {
				username = URLDecoder.decode(localpar[1]);
			} else if (localpar[0].equals(FriendModel.USER_KEY)) {
				userKey = URLDecoder.decode(localpar[1]);
			} else if (localpar[0].equals(FriendModel.MESSAGE)) {
				msg = URLDecoder.decode(localpar[1]);
			}
		}
		Log.i("Message received in service", message);

		FriendModel friend = FriendController.checkFriend(username, userKey);
		if (friend != null) {
			Intent i = new Intent(TAKE_MESSAGE);

			i.putExtra(FriendModel.USERNAME, friend.userName);
			i.putExtra(FriendModel.MESSAGE, msg);
			sendBroadcast(i);
			String activeFriend = FriendController.getActiveFriend();
			if (activeFriend == null || activeFriend.equals(username) == false) {
				showNotification(username, msg);
			}

		}

	}

	private String getAuthenticateUserParams(String usernameText,
			String passwordText) {
		String params = "username="
				+ URLEncoder.encode(usernameText)
				+ "&password="
				+ URLEncoder.encode(passwordText)
				+ "&action="
				+ URLEncoder.encode("authenticateUser")
				+ "&port="
				+ URLEncoder.encode(Integer.toString(socketOperator
						.getListeningPort())) + "&";

		return params;
	}

	private String concatMessageParams(String sender, String receiver,
			String message) {
		Log.i("message params", sender + receiver + message);
		String params = "sender=" + URLEncoder.encode(sender) + "&receiver="
				+ URLEncoder.encode(receiver) + "&action="
				+ URLEncoder.encode("insert_message_for_user") + "&message="
				+ URLEncoder.encode(message) + "&";

		return params;
	}

	private String concatMessageRequestParams(String receiver) {
		String params = "messageReceiver=" + URLEncoder.encode(receiver)
				+ "&action=" + URLEncoder.encode("get_message") + "&";

		return params;
	}

	public void setUserKey(String value) {
		this.userKey = value;
	}

	public boolean isNetworkConnected() {
		if (conManager.getActiveNetworkInfo() != null) {
			return conManager.getActiveNetworkInfo().isConnected();

		} else {
			return false;
		}

	}

	public boolean isUserAuthenticated() {
		return authenticatedUser;
	}

	public String getLastRawFriendList() {
		return this.rawFriendList;
	}

	@Override
	public void onDestroy() {

		super.onDestroy();
	}

	public void exit() {
		if (timer != null)
			timer.cancel();

		// socketOperator.exit();
		Log.i("friends", "service exited");
		authenticatedUser = false;
		// socketOperator = null;
		this.stopSelf();
	}

	public String signUpUser(String usernameText, String passwordText,
			String emailText) {
		String params = "username=" + usernameText + "&password="
				+ passwordText + "&action=" + "signUpUser" + "&email="
				+ emailText + "&";

		String result = socketOperator.sendHttpRequest(params);

		return result;
	}

	public String addNewFriendRequest(String friendUsername) {
		String params = "username=" + this.username + "&password="
				+ this.password + "&action=" + "addNewFriend"
				+ "&friendUserName=" + friendUsername + "&";

		String result = socketOperator.sendHttpRequest(params);

		return result;
	}

	public String sendFriendsReqsResponse(String approvedFriendNames,
			String discardedFriendNames) {
		String params = "username=" + this.username + "&password="
				+ this.password + "&action=" + "responseOfFriendReqs"
				+ "&approvedFriends=" + approvedFriendNames
				+ "&discardedFriends=" + discardedFriendNames + "&";

		String result = socketOperator.sendHttpRequest(params);

		return result;

	}

	private void parseFriendInfo(String xml) {
		try {
			Log.i("friendxmlresult", xml);
			SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
			sp.parse(new ByteArrayInputStream(xml.getBytes()),
					new XMLFriendParser(IMService.this));
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void updateData(FriendModel[] friends,
			FriendModel[] unApprovedFriends, String userKey) {
		this.setUserKey(userKey);

		FriendController.setFriendsInfo(friends);
		FriendController.setUnapprovedFriendsInfo(unApprovedFriends);

	}

	public void updateMessageListData(Vector<MessageModel> messages) {

		MessageController.setMessages(messages);
	}

	public void getDebugList(Vector<Debug> debugList) {
		for (int i = 0; i < debugList.size(); i++) {
			Log.i("debug", debugList.get(i).getMessageUpdated());

		}

	}

	public OtrEngineImpl getHostOtrEngineIml() {
		return hostOtrEngineIml;
	}

	public void setHostOtrEngineIml(OtrEngineImpl hostOtrEngineIml) {
		this.hostOtrEngineIml = hostOtrEngineIml;
	}

}