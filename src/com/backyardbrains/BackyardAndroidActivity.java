package com.backyardbrains;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.MicListener;
import com.backyardbrains.drawing.OscilloscopeGLSurfaceView;

/**
 * Primary activity of the Backyard Brains app. By default shows the continuous
 * oscilloscope view for use with the spikerbox
 * 
 * @author Nathan Dotz <nate@backyardbrains.com>
 * @version 1
 * 
 */
public class BackyardAndroidActivity extends Activity {

	/**
	 * Reference to the {@link OscilloscopeGLSurfaceView} to draw in this
	 * activity
	 */
	private OscilloscopeGLSurfaceView mAndroidSurface;
	/**
	 * Reference to the {@link BackyardBrainsApplication} for message passing
	 */
	private BackyardBrainsApplication application;
	private TextView msView;
	private ImageButton mRecordButton;

	/**
	 * Create the surface we'll use to draw on, grab an instance of the
	 * {@link BackyardBrainsApplication} and use it to spin up the
	 * {@link MicListener} thread (via {@link AudioService}).
	 * 
	 * @TODO remove double-instantiation of mAndroidSurface :O
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.backyard_main);

		// get application
		application = (BackyardBrainsApplication) getApplication();
		application.startAudioService();

		msView = (TextView) findViewById(R.id.millisecondsView);

		// Create custom surface
		mAndroidSurface = new OscilloscopeGLSurfaceView(this);
		FrameLayout mainscreenGLLayout = (FrameLayout) findViewById(R.id.glContainer);
		mainscreenGLLayout.addView(mAndroidSurface);

		mRecordButton = (ImageButton) findViewById(R.id.recordButton);
		mRecordButton.setOnClickListener(
				new OnClickListener() {
					@Override public void onClick(View v) { toggleRecording(); }
				});
		
		IntentFilter intentFilter = new IntentFilter("BYBUpdateMillisecondsReciever");
		UpdateMillisecondsReciever upmillirec = new UpdateMillisecondsReciever();
		registerReceiver(upmillirec, intentFilter);
	}

	protected void toggleRecording() {
		// TODO Auto-generated method stub
		
	}

	private class UpdateMillisecondsReciever extends BroadcastReceiver {
		@Override
		public void onReceive(android.content.Context context,
				android.content.Intent intent) {
			msView.setText(intent.getStringExtra("millisecondsDisplayedString"));
		};
	}

	/**
	 * inflate menu to switch between continuous and threshold modes
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.expandX:
			mAndroidSurface.growXdimension();
			return true;
		case R.id.shrinkX:
			mAndroidSurface.shrinkXdimension();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/**
	 * Attach to {@link AudioService} when we start
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// Bind to LocalService has been moved to OpenGLThread
	}

	/**
	 * Un-bind from {@link AudioService} when we stop
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		// Unbind from the service has been moved to OpenGLThread
		application.stopAudioService();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mAndroidSurface.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	public void setDisplayedMilliseconds(Float ms) {
		msView.setText(ms.toString());
	}
}