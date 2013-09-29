package com.im.otr;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import com.im.interfaces.IAppManager;
import com.im.services.IMService;
import com.im.utils.OtrDebugLogger;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrKeyManagerListener;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.session.SessionID;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/*
 * OtrEngineHostImpl is the connects this app and the OtrEngine
 * http://code.google.com/p/otr4j/wiki/QuickStart
 */
public class OtrEngineHostImpl implements OtrEngineHost {

	private OtrPolicy mPolicy;

	private OtrAndroidKeyManagerImpl mOtrKeyManager;

	private final static String OTR_KEYSTORE_PATH = "otr_keystore";

	private IAppManager mService;
	private Context context;

	public IAppManager getmService() {
		return mService;
	}

	public  void setmService(IAppManager mService) {
		this.mService = mService;
	}

	private Hashtable<SessionID, String> mSessionResources;

	public OtrEngineHostImpl(OtrPolicy policy,
			Context context) throws IOException {
		mPolicy = policy;
		this.context=context;
		mSessionResources = new Hashtable<SessionID, String>();

		File storeFile = new File(
				context.getApplicationContext().getFilesDir(),
				OTR_KEYSTORE_PATH);
		mOtrKeyManager = OtrAndroidKeyManagerImpl.getInstance(storeFile);

		mOtrKeyManager.addListener(new OtrKeyManagerListener() {
			public void verificationStatusChanged(SessionID session) {
				String msg = session + ": verification status="
						+ mOtrKeyManager.isVerified(session);

				OtrDebugLogger.log(msg);
			}

			public void remoteVerifiedUs(SessionID session) {
				String msg = session + ": remote verified us";

				OtrDebugLogger.log(msg);
				if (!isRemoteKeyVerified(session)) {

				}
				// showWarning(session,
				// mContext.getApplicationContext().getString(R.string.remote_verified_us));
			}
		});

	}

	public void putSessionResource(SessionID session, String resource) {
		mSessionResources.put(session, resource);
	}

	public void removeSessionResource(SessionID session) {
		mSessionResources.remove(session);
	}

	public OtrAndroidKeyManagerImpl getKeyManager() {
		return mOtrKeyManager;
	}

	public void storeRemoteKey(SessionID sessionID, PublicKey remoteKey) {
		mOtrKeyManager.savePublicKey(sessionID, remoteKey);
	}

	public boolean isRemoteKeyVerified(SessionID sessionID) {
		return mOtrKeyManager.isVerified(sessionID);
	}

	public String getLocalKeyFingerprint(SessionID sessionID) {
		return mOtrKeyManager.getLocalFingerprint(sessionID);
	}

	public String getRemoteKeyFingerprint(SessionID sessionID) {
		return mOtrKeyManager.getRemoteFingerprint(sessionID);
	}

	public KeyPair getKeyPair(SessionID sessionID) {
		KeyPair kp = null;
		kp = mOtrKeyManager.loadLocalKeyPair(sessionID);

		if (kp == null) {
			mOtrKeyManager.generateLocalKeyPair(sessionID);
			kp = mOtrKeyManager.loadLocalKeyPair(sessionID);
		}
		return kp;
	}

	public OtrPolicy getSessionPolicy(SessionID sessionID) {
		return mPolicy;
	}

	public void setSessionPolicy(OtrPolicy policy) {
		mPolicy = policy;
	}

	private void sendMessage(SessionID sessionID, String body) {
		String receiver = sessionID.getUserID();
		Log.i("message", body);
		Log.i("message","friend username is:"+receiver);
		Log.i("message","acount username is:"+sessionID.getAccountID());
		if(mService==null){
			Log.i("service","service null");
		}else{
			Log.i("service","service  not null");

		}
		Log.i("handshake","sent messages "+body);
		mService.sendMessageForUser(sessionID.getAccountID(), receiver, body);
 
	}

	public void injectMessage(SessionID sessionID, String text) {
		OtrDebugLogger.log(sessionID.toString() + ": injecting message: "
				+ text);
		//Toast.makeText(context, text,Toast.LENGTH_SHORT).show();

		sendMessage(sessionID, text);
	}

	public void showError(SessionID sessionID, String error) {
		OtrDebugLogger.log(sessionID.toString() + ": ERROR=" + error);

		showToast("ERROR: " + error);
	}

	public void showWarning(SessionID sessionID, String warning) {
		OtrDebugLogger.log(sessionID.toString() + ": WARNING=" + warning);

		showToast("WARNING: " + warning);
	}

	/*
	 * private void showDialog(String title, String msg) { Intent nIntent = new
	 * Intent(mContext.getApplicationContext(), WarningDialogActivity.class);
	 * nIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	 * 
	 * nIntent.putExtra("title", title); nIntent.putExtra("msg", msg);
	 * 
	 * mContext.getApplicationContext().startActivity(nIntent); }
	 */
	private void showToast(String msg) {

	}
}
