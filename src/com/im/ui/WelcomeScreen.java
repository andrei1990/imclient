package com.im.ui;

import com.im.R;
import com.im.services.IMService;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

public class WelcomeScreen extends Activity implements OnClickListener {
	private LinearLayout loginControl;
	private LinearLayout signUpControl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_sim);
		startService(new Intent(WelcomeScreen.this, IMService.class));

		loginControl = (LinearLayout) findViewById(R.id.loginControl);
		signUpControl = (LinearLayout) findViewById(R.id.registerControl);
		loginControl.setOnClickListener(this);
		signUpControl.setOnClickListener(this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcome_screen, menu);
		return true;
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.loginControl:
			Intent loginStartIntent=new Intent(WelcomeScreen.this,Login.class);
			startActivity(loginStartIntent);
			finish();

			break;
		case R.id.registerControl:
			Intent registerStartIntent=new Intent(WelcomeScreen.this,SignUp.class);
			startActivity(registerStartIntent);
			finish();

			break;

		default:
			break;
		}

	}

}
