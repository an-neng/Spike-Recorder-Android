package com.backyardbrains.drawing;

public class TriggerViewThread extends OscilloscopeGLThread {

	TriggerViewThread(OscilloscopeGLSurfaceView view) {
		super(view);
	}

	public final boolean drawThreshholdLine = true;
}
