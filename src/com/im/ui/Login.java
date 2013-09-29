package com.im.ui;

import java.io.IOException;

import net.java.otr4j.OtrEngine;
import net.java.otr4j.OtrEngineImpl;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.im.R;
import com.im.app.ImOtrApp;
import com.im.communication.CommunicationManager;
import com.im.interfaces.IAppManager;
import com.im.otr.OtrEngineHostImpl;
import com.im.services.IMService;

public class Login extends Activity {

	protected static final int NOT_CONNECTED_TO_SERVICE = 0;
	protected static final int FILL_BOTH_USERNAME_AND_PASSWORD = 1;
	public static final String AUTHENTICATION_FAILED = "0";
	public static final String FRIEND_LIST = "FRIEND_LIST";
	protected static final int MAKE_SURE_USERNAME_AND_PASSWORD_CORRECT = 2;
	private static final int APLICATION_SERVER_NOT_RESPONING = 12;
	protected static final int NOT_CONNECTED_TO_NETWORK = 3;
	private EditText usernameText;
	private EditText passwordText;
	private Button cancelButton;
	private IAppManager imService;
	public static final int SIGN_UP_ID = Menu.FIRST;
	public static final int EXIT_APP_ID = Menu.FIRST + 1;
	public static OtrEngine otrEngineIml;
	ProgressDialog loginProgessDialog;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			imService = ((IMService.IMBinder) service).getService();

			if (imService.isUserAuthenticated() == true) {
				Intent i = new Intent(Login.this, FriendList.class);
				startActivity(i);
				Login.this.finish();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			imService = null;
			Toast.makeText(Login.this, R.string.local_service_stopped,
					Toast.LENGTH_SHORT).show();
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ImOtrApp.initiatedSessions.clear();
		/*
		 * Start and bind the imService
		 */
		startService(new Intent(Login.this, IMService.class));
		Log.i("friends", "logincreated");
		setContentView(R.layout.login_screen);
		setTitle("Login");

		Button loginButton = (Button) findViewById(R.id.login);
		cancelButton = (Button) findViewById(R.id.cancel_login);
		usernameText = (EditText) findViewById(R.id.userName);
		passwordText = (EditText) findViewById(R.id.password);

		loginButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				Log.i("friends", "clicked");

				if (imService == null) {
					showDialog(NOT_CONNECTED_TO_SERVICE);
					return;
				} else if (imService.isNetworkConnected() == false) {
					showDialog(NOT_CONNECTED_TO_NETWORK);

				} else if (usernameText.length() > 0
						&& passwordText.length() > 0) {
					showProgressDialog();
					Thread loginThread = new Thread() {
						private Handler handler = new Handler();

						@Override
						public void run() {
							// ImOtrApp.getImOtrApp().getOtrEngineHostImpl().setmService(imService);
							String result = imService.authenticateUser(
									usernameText.getText().toString(),
									passwordText.getText().toString());

							Log.i("result", "result is :" + result);
							if (result == null
									|| result.equals(AUTHENTICATION_FAILED)) {
								/*
								 * Authenticatin failed, inform the user
								 */
								handler.post(new Runnable() {
									public void run() {
										dismissProgressDialog();
										showDialog(MAKE_SURE_USERNAME_AND_PASSWORD_CORRECT);
									}
								});

							} else if (result
									.equals(CommunicationManager.HTTP_REQUEST_FAILED)) {

								/*
								 * Aplication server is not runing or is broken
								 */
								handler.post(new Runnable() {
									public void run() {
										dismissProgressDialog();
										showDialog(APLICATION_SERVER_NOT_RESPONING);
									}
								});

							} else {
								handler.post(new Runnable() {
									public void run() {
										dismissProgressDialog();

										// create new otrengineimplementation
										otrEngineIml = new OtrEngineImpl(
												ImOtrApp.getImOtrApp()
														.getOtrEngineHostImpl(),
												getApplicationContext());
										Log.i("loged user",
												imService.getUsername());
										Log.i("friends", "handler entered");
										Intent i = new Intent(Login.this,
												FriendList.class);
										// i.putExtra(FRIEND_LIST, result);
										startActivity(i);
										finish();
									}
								});
							}

						}
					};
					loginThread.start();

				} else {
					/*
					 * Username or Password is not filled, alert the user
					 */
					showDialog(FILL_BOTH_USERNAME_AND_PASSWORD);
				}
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				// imService.exit();
				finish();
				Intent welcomeIntent = new Intent(Login.this,
						WelcomeScreen.class);
				startActivity(welcomeIntent);

			}

		});

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		int message = -1;
		switch (id) {
		case NOT_CONNECTED_TO_SERVICE:
			message = R.string.not_connected_to_service;
			break;
		case FILL_BOTH_USERNAME_AND_PASSWORD:
			message = R.string.fill_both_username_and_password;
			break;
		case MAKE_SURE_USERNAME_AND_PASSWORD_CORRECT:
			message = R.string.make_sure_username_and_password_correct;
			break;
		case NOT_CONNECTED_TO_NETWORK:
			message = R.string.not_connected_to_network;
			break;
		case APLICATION_SERVER_NOT_RESPONING:
			message = R.string.aplication_server_not_responding;
			break;
		default:
			break;
		}

		if (message == -1) {
			return null;
		} else {
			return new AlertDialog.Builder(Login.this)
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
		unbindService(mConnection);
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.i("friends", "loginresumed");
		bindService(new Intent(Login.this, IMService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		// ImOtrApp.getImOtrApp().getOtrEngineHostImpl().setmService(imService);
		super.onResume();
	}

	private void showProgressDialog() {
		loginProgessDialog = new ProgressDialog(Login.this);
		loginProgessDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		loginProgessDialog.setMessage("Signing in...");
		loginProgessDialog.show();
	}

	private void dismissProgressDialog() {
		if (loginProgessDialog != null)
			loginProgessDialog.dismiss();
	}

	
}