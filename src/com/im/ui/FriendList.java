package com.im.ui;

import java.util.Vector;

import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionStatus;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.im.R;
import com.im.app.ImOtrApp;
import com.im.controllers.FriendController;
import com.im.controllers.MessageController;
import com.im.interfaces.IAppManager;
import com.im.models.FriendModel;
import com.im.models.MessageModel;
import com.im.services.IMService;
import com.im.utils.STATUS;

public class FriendList extends ListActivity implements OnClickListener {
	private static final int ADD_NEW_FRIEND_ID = Menu.FIRST;
	private static final int EXIT_APP_ID = Menu.FIRST + 1;
	private IAppManager imService = null;
	private FriendListAdapter friendAdapter;
	public static final String ENCRYPTED_CONVERSATION = "encrypted_conversation";
	public static final String SENDER_NAME = "sender_name";
	public static final String CANCEL_CONVERSATION_CREATION = "cancel_conversation_creation";
	public static final String SHOW_QUERY_DIALOG = "show_query_dialog";
	Intent messagingIntent;
	ProgressDialog encryptingNotificationDialog;
	SessionID sessionId;
	private ImageView addNewFriend;
	private ImageView exitApp;
	private Context context;

	private class FriendListAdapter extends BaseAdapter {
		class ViewHolder {
			TextView text;
			ImageView icon;
		}

		private LayoutInflater mInflater;
		private Bitmap mOnlineIcon;
		private Bitmap mOfflineIcon;

		private FriendModel[] friends = null;

		public FriendListAdapter(Context context) {
			super();

			mInflater = LayoutInflater.from(context);

			mOnlineIcon = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.online);
			mOfflineIcon = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.offline);

		}

		public void setFriendList(FriendModel[] friends) {
			this.friends = friends;
		}

		public int getCount() {

			return friends.length;
		}

		public FriendModel getItem(int position) {

			return friends[position];
		}

		public long getItemId(int position) {

			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.friend_list_screen,
						null);

				// Creates a ViewHolder and store references to the two children
				// views
				// we want to bind data to.
				holder = new ViewHolder();
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);

				convertView.setTag(holder);
			} else {
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ViewHolder) convertView.getTag();
			}

			// Bind the data efficiently with the holder.
			holder.text.setText(friends[position].userName);
			holder.icon
					.setImageBitmap(friends[position].status == STATUS.ONLINE ? mOnlineIcon
							: mOfflineIcon);

			return convertView;
		}

	}

	public class MessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			Bundle extra = intent.getExtras();
			if (extra != null) {
				String action = intent.getAction();
				if (action.equals(IMService.FRIEND_LIST_UPDATED)) {

					FriendList.this.updateData(
							FriendController.getFriendsInfo(),
							FriendController.getUnapprovedFriendsInfo());

				} else if (action.equals(IMService.UPDATE_MESSAGE_HISTORY)) {

					Vector<MessageModel> messages = MessageController
							.getMessages();
					if (messages.size() > 0) {
						MessageModel messageModel = messages.get(0);
						SessionID sesId = new SessionID(
								imService.getUsername(),
								messageModel.getSender(), null);
						if (Login.otrEngineIml.getSessionStatus(sesId) == SessionStatus.ENCRYPTED) {

							SessionID sessionId = new SessionID(
									imService.getUsername(),
									messageModel.getSender(), null);
							String decryptedMessage = "";
							try {
								decryptedMessage = Login.otrEngineIml
										.transformReceiving(sessionId,
												messageModel.getImMessage());
								NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

								Notification notification = new Notification(
										R.drawable.greenstar,
										messageModel.getSender() + ":"
												+ decryptedMessage,
										System.currentTimeMillis());

								Intent i = new Intent(FriendList.this,
										Messaging.class);
								i.putExtra(FriendModel.USERNAME,
										messageModel.getSender());
								i.putExtra(FriendModel.MESSAGE,
										decryptedMessage);
								PendingIntent contentIntent = PendingIntent
										.getActivity(FriendList.this, 0, i, 0);
								notification.setLatestEventInfo(
										FriendList.this,
										messageModel.getSender()
												+ decryptedMessage,
										messageModel.getSender() + ":"
												+ decryptedMessage,
										contentIntent);

								NM.notify(1, notification);
							} catch (OtrException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							Log.i("message sender", messageModel.getSender());
						}

					}
				}
			}
		}

	};

	public class ConversationStatusReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (action.equals(FriendList.ENCRYPTED_CONVERSATION)) {

				dismissProgressDialog();
				if (messagingIntent == null) {
					String senderName = intent
							.getStringExtra(FriendList.SENDER_NAME);
					messagingIntent = new Intent(FriendList.this,
							Messaging.class);
					messagingIntent.putExtra(FriendModel.USERNAME, senderName);
					startActivity(messagingIntent);

				} else {
					startActivity(messagingIntent);

				}
			} else if (action.equals(FriendList.CANCEL_CONVERSATION_CREATION)) {
				dismissProgressDialog();

			} else if (action.equals(FriendList.SHOW_QUERY_DIALOG)) {
				AlertDialog.Builder builder1 = new AlertDialog.Builder(context)
						.setTitle("OTR")
						.setMessage(
								getResources().getString(R.string.otrquery)
										+ "  "
										+ ImOtrApp.requestConversationSender
										+ "?")
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										try {
											showProgressDialog(ImOtrApp.requestConversationSender);
											sessionId = new SessionID(
													imService.getUsername(),
													ImOtrApp.requestConversationSender,
													null);
											Login.otrEngineIml
													.getSession(sessionId)
													.getAuthContext()
													.respondV2Auth();
										} catch (OtrException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								})
						.setNegativeButton(R.string.no,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										// do nothing
										sessionId = new SessionID(
												imService.getUsername(),
												ImOtrApp.requestConversationSender,
												null);

										ImOtrApp.getImOtrApp()
												.getOtrEngineHostImpl()
												.injectMessage(sessionId,
														"This user refused to start an off the record messaging");
									}
								});
				AlertDialog alert11 = builder1.create();
				alert11.show();
			}
		}

	};

	public MessageReceiver messageReceiver = new MessageReceiver();
	public ConversationStatusReceiver convStatusReceiver = new ConversationStatusReceiver();

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			imService = ((IMService.IMBinder) service).getService();

			FriendModel[] friends = FriendController.getFriendsInfo();
			if (friends != null) {
				FriendList.this.updateData(friends, null);
			}

			setTitle(imService.getUsername() + "'s friend list");
		}

		public void onServiceDisconnected(ComponentName className) {
			imService = null;
			Toast.makeText(FriendList.this, R.string.local_service_stopped,
					Toast.LENGTH_SHORT).show();
		}
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_screen);
		context = this;
		exitApp = (ImageView) findViewById(R.id.exitApp);
		addNewFriend = (ImageView) findViewById(R.id.add_friend);
		exitApp.setOnClickListener(this);
		addNewFriend.setOnClickListener(this);
		Log.i("friends", "friendscreated");
		friendAdapter = new FriendListAdapter(this);
	}

	public void updateData(FriendModel[] friends,
			FriendModel[] unApprovedFriends) {
		if (friends != null) {
			friendAdapter.setFriendList(friends);
			setListAdapter(friendAdapter);
		}

		if (unApprovedFriends != null) {
			NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			if (unApprovedFriends.length > 0) {
				String tmp = new String();
				for (int j = 0; j < unApprovedFriends.length; j++) {
					tmp = tmp.concat(unApprovedFriends[j].userName).concat(",");
				}
				Notification notification = new Notification(
						R.drawable.greenstar,
						getText(R.string.new_friend_request_exist),
						System.currentTimeMillis());

				Intent i = new Intent(this, FriendRequestResponseActivity.class);
				i.putExtra(FriendModel.FRIEND_LIST, tmp);

				PendingIntent contentIntent = PendingIntent.getActivity(this,
						0, i, 0);

				notification.setLatestEventInfo(this,
						getText(R.string.new_friend_request_exist),
						"You have new friend request(s)", contentIntent);

				NM.notify(R.string.new_friend_request_exist, notification);
			} else {
				// if any request exists, then cancel it
				NM.cancel(R.string.new_friend_request_exist);
			}
		}

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		super.onListItemClick(l, v, position, id);
		// ImOtrApp.getImOtrApp().getOtrEngineHostImpl().setmService(imService);

		messagingIntent = new Intent(this, Messaging.class);
		FriendModel friend = friendAdapter.getItem(position);
		if (friend.status == STATUS.ONLINE) {
			// // start encrypted session
			if (ImOtrApp.initiatedSessions.get(friend.userName) == null) {
				showProgressDialog(friend.userName);
				Log.i("test", "testing");
				sessionId = new SessionID(imService.getUsername(),
						friend.userName, null);
				messagingIntent.putExtra(FriendModel.USERNAME, friend.userName);
				messagingIntent.putExtra(FriendModel.PORT, friend.port);
				messagingIntent.putExtra(FriendModel.IP, friend.ip);
				try {
					Login.otrEngineIml.startSession(sessionId);
					// Login.otrEngineIml.getSession(sessionId).getAuthContext()
					// .respondV2Auth();
				} catch (OtrException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				messagingIntent.putExtra(FriendModel.USERNAME, friend.userName);
				startActivity(messagingIntent);

			}

		} else {
			Toast.makeText(FriendList.this, R.string.user_offline,
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onPause() {
		unregisterReceiver(messageReceiver);
		unregisterReceiver(convStatusReceiver);

		unbindService(mConnection);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i("friends", "friendsActivityResumed");
		bindService(new Intent(FriendList.this, IMService.class), mConnection,
				Context.BIND_AUTO_CREATE);

		// ImOtrApp.getImOtrApp().getOtrEngineHostImpl().setmService(imService);
		IntentFilter i = new IntentFilter();

		i.addAction(IMService.FRIEND_LIST_UPDATED);
		i.addAction(IMService.UPDATE_MESSAGE_HISTORY);
		IntentFilter conversation = new IntentFilter();

		conversation.addAction(FriendList.ENCRYPTED_CONVERSATION);
		conversation.addAction(FriendList.CANCEL_CONVERSATION_CREATION);
		conversation.addAction(FriendList.SHOW_QUERY_DIALOG);

		registerReceiver(messageReceiver, i);
		registerReceiver(convStatusReceiver, conversation);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);

	}

	private void showProgressDialog(String userName) {
		encryptingNotificationDialog = new ProgressDialog(FriendList.this);
		encryptingNotificationDialog
				.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		encryptingNotificationDialog.setMessage(getResources().getString(
				R.string.initConversation)
				+ " " + userName);
		encryptingNotificationDialog.show();
	}

	private void dismissProgressDialog() {
		if (encryptingNotificationDialog != null)
			encryptingNotificationDialog.dismiss();
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.exitApp:
			AlertDialog.Builder builder1 = new AlertDialog.Builder(context)
					.setTitle(R.string.exitApp)
					.setMessage(R.string.dialogMessage)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									imService.exit();
									if (Login.otrEngineIml != null) {
										if (Login.otrEngineIml.getSessions() != null) {
											Login.otrEngineIml.getSessions()
													.clear();
										}
									}
									finish();
									Intent welcomeIntent = new Intent(
											FriendList.this,
											WelcomeScreen.class);
									startActivity(welcomeIntent);

								}
							})
					.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();

								}
							});
			AlertDialog alert11 = builder1.create();
			alert11.show();
			break;
		case R.id.add_friend:
			Intent i = new Intent(FriendList.this, AddFriend.class);
			startActivity(i);

			break;

		default:
			break;
		}

	}

}
