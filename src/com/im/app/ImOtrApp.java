package com.im.app;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.SessionID;

import com.im.R;
import com.im.controllers.FriendController;
import com.im.controllers.MessageController;
import com.im.interfaces.IAppManager;
import com.im.models.FriendModel;
import com.im.models.MessageModel;
import com.im.otr.OtrEngineHostImpl;
import com.im.services.IMService;
import com.im.ui.FriendList;
import com.im.ui.Login;
import com.im.ui.Messaging.MessageReceiver;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ImOtrApp extends Application {

	private static OtrEngineHost otrEngineHostImpl;
	private IAppManager imService;
	private static ImOtrApp imOtrApp;
	public static boolean WAS_CONVERSATION_ENCRYPTED = false;
	public static HashMap<String, String> initiatedSessions;
	public static String requestConversationSender=null;

	@Override
	public void onCreate() {
		startService(new Intent(getApplicationContext(), IMService.class));
		bindService(new Intent(getApplicationContext(), IMService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		Log.i("application process created", "createad");
		imOtrApp = new ImOtrApp();
		initiatedSessions = new HashMap<String, String>();
		OtrPolicy otrPolicy = new OtrPolicyImpl(OtrPolicy.ALLOW_V2
				| OtrPolicy.ERROR_START_AKE);
		try {
			setOtrEngineHostImpl(new OtrEngineHostImpl(otrPolicy,
					getApplicationContext()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// otrEngineHostImpl.setmService(imService);
		IntentFilter i = new IntentFilter();
		i.addAction(IMService.UPDATE_MESSAGE_HISTORY);
		registerReceiver(messageReceiver, i);
		super.onCreate();
	}

	public static ImOtrApp getImOtrApp() {
		return imOtrApp;
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
						if (messages!=null&&messages.size() > 0) {
							// Toast.makeText(getApplicationContext(),
							// "Message received", Toast.LENGTH_SHORT)
							// .show();

							MessageModel messageModel = messages.get(0);
							
							if (initiatedSessions.get(messageModel.getSender()) == null) {
								String message = messageModel.getImMessage();
								
								if(!message.equals( "This user refused to start an off the record messaging")){
                                
								SessionID sesionId = new SessionID(
										imService.getUsername(),
										messageModel.getSender(), null);
								requestConversationSender=messageModel.getSender();
								try {
									Login.otrEngineIml.transformReceiving(
											sesionId, message);
								} catch (OtrException e) {
									Log.i("otr exception", e.toString());
									e.printStackTrace();
								}
								
							}else{
								Intent cancelConversationEncryption=new Intent(FriendList.CANCEL_CONVERSATION_CREATION);
								sendBroadcast(cancelConversationEncryption);
							}
							}
						}
					}
				}

			
		}

	};

	private MessageReceiver messageReceiver = new MessageReceiver();

	public OtrEngineHost getOtrEngineHostImpl() {
		return otrEngineHostImpl;
	}

	public void setOtrEngineHostImpl(OtrEngineHostImpl otrEngineHostImpl) {
		this.otrEngineHostImpl = otrEngineHostImpl;
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			imService = ((IMService.IMBinder) service).getService();
			if (imService != null) {
				Log.i("service", "service  runing");
				otrEngineHostImpl.setmService(imService);
			} else {
				Log.i("service", "service not runing");

			}

		}

		public void onServiceDisconnected(ComponentName className) {
			imService = null;

		};
	};

}
