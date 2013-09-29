package com.im.ui;

import java.util.Vector;

import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionID;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.im.R;
import com.im.app.ImOtrApp;
import com.im.controllers.FriendController;
import com.im.controllers.MessageController;
import com.im.interfaces.IAppManager;
import com.im.models.FriendModel;
import com.im.models.MessageModel;
import com.im.services.IMService;

public class Messaging extends Activity {

	private static final int MESSAGE_CANNOT_BE_SENT = 0;
	public static final String GET_NEW_MESSAGE = "get_new_message";
	private EditText messageText;
	private EditText messageHistoryText;
	private Button sendMessageButton;
	private IAppManager imService;
	private FriendModel friend = new FriendModel();

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			imService = ((IMService.IMBinder) service).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			imService = null;
			Toast.makeText(Messaging.this, R.string.local_service_stopped,
					Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.messaging_screen); // messaging_screen);

		messageHistoryText = (EditText) findViewById(R.id.messageHistory);
		messageHistoryText.append(getResources().getString(R.string.conversationString) + "\n");

		messageText = (EditText) findViewById(R.id.message);

		messageText.requestFocus();

		sendMessageButton = (Button) findViewById(R.id.sendMessageButton);

		Bundle extras = this.getIntent().getExtras();
		Intent intent=getIntent();

		friend.userName = intent.getStringExtra(FriendModel.USERNAME);
		Log.i("friendUsername", "friendUsername is  :" + friend.userName);
		friend.ip = extras.getString(FriendModel.IP);
		friend.port = extras.getString(FriendModel.PORT);
		String msg = intent.getStringExtra(FriendModel.MESSAGE);
		setTitle("Messaging with " + friend.userName);

		((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
		.cancel(1);
		if (msg != null) {
			Log.i("message",msg);
			this.appendToMessageHistory(friend.userName, msg);
			
		}

		sendMessageButton.setOnClickListener(new OnClickListener() {
			CharSequence message;
			Handler handler = new Handler();

			public void onClick(View arg0) {
				message = messageText.getText();
				final String message=messageText.getText().toString();
				if (message.length() > 0) {
					appendToMessageHistory(imService.getUsername(),
							message.toString());

					messageText.setText("");
					Thread thread = new Thread() {
						public void run() {
							SessionID sessionId = new SessionID(imService
									.getUsername(), friend.userName, null);
							try {
								String messageToBeSent = Login.otrEngineIml
										.transformSending(sessionId,
												message.toString());
								ImOtrApp.getImOtrApp()
										.getOtrEngineHostImpl()
										.injectMessage(sessionId,
												messageToBeSent);
							} catch (OtrException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							// {
							//
							// handler.post(new Runnable(){
							//
							// public void run() {
							// showDialog(MESSAGE_CANNOT_BE_SENT);
							// }
							//
							// });
							// }
						}
					};
					thread.start();

				}

			}
		});

		messageText.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == 66) {
					sendMessageButton.performClick();
					return true;
				}
				return false;
			}

		});

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		int message = -1;
		switch (id) {
		case MESSAGE_CANNOT_BE_SENT:
			message = R.string.message_cannot_be_sent;
			break;
		}

		if (message == -1) {
			return null;
		} else {
			return new AlertDialog.Builder(Messaging.this)
					.setMessage(message)
					.setPositiveButton(R.string.OK,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									/* User clicked OK so do some stuff */
								}
							}).create();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(messageReceiver);
		unbindService(mConnection);

		FriendController.setActiveFriend(null);

	}

	@Override
	protected void onResume() {
		super.onResume();
		bindService(new Intent(Messaging.this, IMService.class), mConnection,
				Context.BIND_AUTO_CREATE);

		IntentFilter i = new IntentFilter();
		i.addAction(IMService.UPDATE_MESSAGE_HISTORY);

		registerReceiver(messageReceiver, i);

		FriendController.setActiveFriend(friend.userName);

	}

	public class MessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle extra = intent.getExtras();
			if (extra != null) {
				String action = intent.getAction();
				if (action.equals(IMService.UPDATE_MESSAGE_HISTORY)) {
					Vector<MessageModel> messages = MessageController
							.getMessages();
					if (messages.size() > 0) {
						MessageModel messageModel = messages.get(0);
						SessionID sessionId = new SessionID(
								imService.getUsername(),
								messageModel.getSender(), null);
						String decryptedMessage = "";
						try {
							decryptedMessage = Login.otrEngineIml
									.transformReceiving(sessionId,
											messageModel.getImMessage());
						} catch (OtrException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Log.i("message sender", messageModel.getSender());
						appendToMessageHistory(messageModel.getSender(),
								decryptedMessage);
					}
				}
			}

		}

	};

	private MessageReceiver messageReceiver = new MessageReceiver();

	private void appendToMessageHistory(String username, String message) {
		if (username != null && message != null) {

			messageHistoryText.append(username + ":\n");
			messageHistoryText.append(message + "\n");
		}
	}

}
