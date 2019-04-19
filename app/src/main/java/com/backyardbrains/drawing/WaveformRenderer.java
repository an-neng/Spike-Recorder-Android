/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.view.MotionEvent;
import com.backyardbrains.drawing.gl.GlDashedHLine;
import com.backyardbrains.drawing.gl.GlEventMarker;
import com.backyardbrains.drawing.gl.GlFft;
import com.backyardbrains.drawing.gl.GlHandle;
import com.backyardbrains.drawing.gl.GlHandleDragHelper;
import com.backyardbrains.drawing.gl.GlWaveform;
import com.backyardbrains.drawing.gl.Rect;
import com.backyardbrains.ui.BaseFragment;
import com.backyardbrains.utils.ArrayUtils;
import com.backyardbrains.utils.JniUtils;
import com.backyardbrains.utils.PrefUtils;
import com.crashlytics.android.Crashlytics;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class WaveformRenderer extends BaseWaveformRenderer {

    private static final String TAG = makeLogTag(WaveformRenderer.class);

    private static final float DASH_SIZE = 30f;
    private static final int LINE_WIDTH = 1;
    private static final float MARKER_LABEL_TOP = 230f;
    private static final float MARKER_LABEL_TOP_OFFSET = 20f;
    // Percentage of the screen that FFT drawing requires
    private static final float FFT_HEIGHT_PERCENTAGE = .6f; // 60%
    // Size of the bottom half of the screen (below zero) when drawing waveform
    private static final float MAX_GL_FFT_VERTICAL_HALF_SIZE = MAX_GL_VERTICAL_HALF_SIZE * 4f;

    private final GlHandleDragHelper waveformHandleDragHelper;
    private final GlHandleDragHelper thresholdHandleDragHelper;
    private final Rect rect = new Rect();

    private final GlWaveform glWaveform;
    private final GlFft glFft;
    private final GlHandle glHandle;
    private final GlDashedHLine glThresholdLine;
    private GlEventMarker glEventMarker;

    private float threshold;
    private float[][] channelColors = new float[][] { Colors.CHANNEL_0.clone() };

    /**
     * Interface definition for a callback to be invoked when one of the drawn waveforms is selected by clicking he
     * waveform handle.
     */
    public interface OnWaveformSelectionListener {
        /**
         * Listener that is invoked when waveform is selected.
         *
         * @param index Index of the selected waveform.
         */
        void onWaveformSelected(int index);
    }

    private OnWaveformSelectionListener listener;

    public WaveformRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();

        waveformHandleDragHelper = new GlHandleDragHelper(new GlHandleDragHelper.OnDragListener() {
            @Override public void onDragStart(int index) {
                selectWaveform(index);
            }

            @Override public void onDrag(int index, float dy) {
                moveGlWindowForSelectedChannel(dy);
            }

            @Override public void onDragStop(int index) {
                // ignore
            }
        });
        thresholdHandleDragHelper = new GlHandleDragHelper(new GlHandleDragHelper.OnDragListener() {
            @Override public void onDragStart(int index) {
                // ignore
            }

            @Override public void onDrag(int index, float dy) {
                updateThreshold(dy);
            }

            @Override public void onDragStop(int index) {
                // ignore
            }
        });

        glWaveform = new GlWaveform();
        glFft = new GlFft();
        glHandle = new GlHandle();
        glThresholdLine = new GlDashedHLine();
    }

    //=================================================
    //  PUBLIC AND PROTECTED METHODS
    //=================================================

    /**
     * Registers a callback to be invoked when one of waveforms is selected.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnWaveformSelectionListener(@Nullable OnWaveformSelectionListener listener) {
        this.listener = listener;
    }

    /**
     * Returns color of all the channels.
     */
    public float[][] getChannelColors() {
        return ArrayUtils.copy(channelColors);
    }

    /**
     * Sets specified {@code color} for the channel at specified {@code channelIndex}.
     */
    public void setChannelColor(int channelIndex, @Size(4) float[] color) {
        System.arraycopy(color, 0, channelColors[channelIndex], 0, color.length);
    }

    /**
     * Sets
     */
    protected void setThreshold(float threshold) {
        if (threshold == 0) return;

        this.threshold = threshold;

        // pass new threshold to the c++ code
        JniUtils.setThreshold(threshold);
    }

    //=================================================
    //  Renderer INTERFACE IMPLEMENTATIONS
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        glEventMarker = new GlEventMarker(context, gl);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

        waveformHandleDragHelper.resetDraggableAreas();
        waveformHandleDragHelper.setSurfaceHeight(height);

        thresholdHandleDragHelper.resetDraggableAreas();
        thresholdHandleDragHelper.setSurfaceHeight(height);
    }

    //=================================================
    //  TouchEnabledRenderer INTERFACE IMPLEMENTATIONS
    //=================================================

    /**
     * {@inheritDoc}
     */
    @Override public boolean onTouchEvent(MotionEvent event) {
        return waveformHandleDragHelper.onTouch(event) || thresholdHandleDragHelper.onTouch(event);
    }

    //=================================================
    //  BaseWaveformRenderer OVERRIDES
    //=================================================

    /**
     * {@inheritDoc}
     *
     * @param channelCount The new number of channels.
     */
    @Override public void onChannelCountChanged(int channelCount) {
        super.onChannelCountChanged(channelCount);

        channelColors = new float[channelCount][];
        for (int i = 0; i < channelCount; i++) {
            channelColors[i] = new float[4];
            setChannelColor(i, Colors.CHANNEL_COLORS[i % Colors.CHANNEL_COLORS.length]);
        }

        // we should reset draggable areas for both waveforms and thresholds
        waveformHandleDragHelper.resetDraggableAreas();
        thresholdHandleDragHelper.resetDraggableAreas();
    }

    /**
     * {@inheritDoc}
     *
     * @param channelConfig Array of booleans indicating which channel is on and which is off.
     */
    @Override public void onChannelConfigChanged(boolean[] channelConfig) {
        super.onChannelConfigChanged(channelConfig);

        for (int i = 0; i < channelConfig.length; i++) {
            if (!channelConfig[i]) setChannelColor(i, Colors.BLACK);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param channelIndex Index of the selected channel.
     */
    @Override public void onChannelSelectionChanged(int channelIndex) {
        thresholdHandleDragHelper.resetDraggableAreas();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onLoadSettings(@NonNull Context context) {
        super.onLoadSettings(context);

        setThreshold(PrefUtils.getThreshold(context, getClass()));
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSaveSettings(@NonNull Context context) {
        super.onSaveSettings(context);

        PrefUtils.setThreshold(context, getClass(), threshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void prepareSignalForDrawing(@NonNull SignalDrawData outSignalData, EventsDrawData outEvents,
        @NonNull short[][] inSamples, int inFrameCount, @NonNull int[] inEventIndices, int inEventCount, int fromSample,
        int toSample, int drawSurfaceWidth) {
        //benchmark.start();
        try {
            if (isSignalAveraging()) {
                JniUtils.prepareForThresholdDrawing(outSignalData, outEvents, inSamples, inFrameCount, inEventIndices,
                    inEventCount, fromSample, toSample, drawSurfaceWidth);
            } else {
                JniUtils.prepareForSignalDrawing(outSignalData, outEvents, inSamples, inFrameCount, inEventIndices,
                    inEventCount, fromSample, toSample, drawSurfaceWidth);
            }
        } catch (Exception e) {
            LOGE(TAG, e.getMessage());
            Crashlytics.logException(e);
        }
        //benchmark.end();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void addEventsToEventDrawData(@NonNull EventsDrawData eventsDrawData,
        @NonNull String[] eventNames, int eventCount) {
        // only process events if threshold is off
        if (!isSignalAveraging()) {
            int indexBase = eventCount - eventsDrawData.eventCount;
            if (indexBase >= 0) {
                if (eventsDrawData.eventCount >= 0) {
                    System.arraycopy(eventNames, indexBase, eventsDrawData.eventNames, 0, eventsDrawData.eventCount);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fftDrawData FFT data prepared for drawing.
     * @param fft Incoming FFT data.
     * @param drawSurfaceWidth Width of the surface FFT data is being drawn to.
     */
    @Override protected void prepareFftForDrawing(@NonNull FftDrawData fftDrawData, @NonNull float[][] fft,
        int drawSurfaceWidth) {
        //benchmark.start();
        try {
            JniUtils.prepareForFftDrawing(fftDrawData, fft, drawSurfaceWidth,
                (int) (MAX_GL_VERTICAL_SIZE * FFT_HEIGHT_PERCENTAGE));
        } catch (Exception e) {
            LOGE(TAG, e.getMessage());
            Crashlytics.logException(e);
        }
        //benchmark.end();
    }

    /**
     * {@inheritDoc}
     */
    @Override protected void draw(GL10 gl, @NonNull short[][] samples, int selectedChannel,
        SignalDrawData signalDrawData, @NonNull EventsDrawData eventsDrawData, @NonNull FftDrawData fftDrawData,
        int surfaceWidth, int surfaceHeight, float glWindowWidth, float[] waveformScaleFactors,
        float[] waveformPositions, int drawStartIndex, int drawEndIndex, float scaleX, float scaleY,
        long lastFrameIndex) {
        final float samplesToDraw = signalDrawData.sampleCounts[0] * .5f;
        final float drawScale = surfaceWidth > 0 ? samplesToDraw / surfaceWidth : 1f;
        final boolean showWaveformHandle = signalDrawData.channelCount > 1;
        final boolean isSignalAveraging = isSignalAveraging();
        final boolean isThresholdSignalAveraging = isThresholdAveragingTriggerType();
        final boolean isFftProcessing = isFftProcessing();
        boolean selected, showThresholdHandle;
        float[] waveformColor;

        if (isFftProcessing) {
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrthof(0f, samplesToDraw - 1, -MAX_GL_FFT_VERTICAL_HALF_SIZE, MAX_GL_VERTICAL_HALF_SIZE, -1f, 1f);
        }

        for (int i = 0; i < signalDrawData.channelCount; i++) {
            selected = getSelectedChanel() == i;
            showThresholdHandle = selected && isSignalAveraging && isThresholdSignalAveraging;
            waveformColor = getWaveformColor(i);

            gl.glPushMatrix();
            gl.glTranslatef(0f, waveformPositions[i], 0f);

            // draw waveform
            gl.glPushMatrix();
            gl.glScalef(1f, waveformScaleFactors[i], 1f);
            glWaveform.draw(gl, signalDrawData.samples[i], signalDrawData.sampleCounts[i], waveformColor);
            gl.glPopMatrix();

            if (showWaveformHandle) {
                // draw waveform handle
                gl.glPushMatrix();
                gl.glScalef(drawScale, scaleY, 1f);
                glHandle.draw(gl, waveformColor, selected);
                gl.glPopMatrix();

                // register waveform handle as draggable area with drag helper
                glHandle.getBorders(rect);
                waveformHandleDragHelper.registerDraggableArea(i, rect.x, rect.y + glYToSurfaceY(waveformPositions[i]),
                    rect.width, rect.height);
            }

            if (showThresholdHandle) {
                float scaledThreshold = threshold * waveformScaleFactors[i];
                if (scaledThreshold + waveformPositions[i] < -MAX_GL_VERTICAL_HALF_SIZE) {
                    scaledThreshold = -MAX_GL_VERTICAL_HALF_SIZE - waveformPositions[i];
                }
                if (scaledThreshold + waveformPositions[i] > MAX_GL_VERTICAL_HALF_SIZE) {
                    scaledThreshold = MAX_GL_VERTICAL_HALF_SIZE - waveformPositions[i];
                }
                // draw threshold line
                gl.glPushMatrix();
                gl.glScalef(drawScale, waveformScaleFactors[i], 1f);
                gl.glTranslatef(0f, threshold, 0f);
                glThresholdLine.draw(gl, 0f, samplesToDraw - 1, DASH_SIZE, LINE_WIDTH, Colors.RED);
                gl.glPopMatrix();
                // draw threshold handle
                gl.glPushMatrix();
                gl.glTranslatef(samplesToDraw - 1, scaledThreshold, 0f);
                gl.glScalef(-drawScale, scaleY, 1f);
                glHandle.draw(gl, Colors.RED, true);
                gl.glPopMatrix();

                // register threshold handle as draggable area with drag helper
                glHandle.getBorders(rect);
                thresholdHandleDragHelper.registerDraggableArea(i, surfaceWidth - rect.width,
                    rect.y + glYToSurfaceY(waveformPositions[i] + scaledThreshold), rect.width, rect.height);
            }

            gl.glPopMatrix();
        }

        // draw markers
        if (!isSignalAveraging) {
            float prevX = 0f, prevLabelYOffset = MARKER_LABEL_TOP;
            for (int i = 0; i < eventsDrawData.eventCount; i++) {
                float x = eventsDrawData.eventIndices[i];
                float labelYOffset = MARKER_LABEL_TOP;
                if (i != 0 && (x - prevX) < rect.width) {
                    labelYOffset = prevLabelYOffset + rect.height + MARKER_LABEL_TOP_OFFSET;
                }

                gl.glPushMatrix();
                gl.glTranslatef(x, -MAX_GL_VERTICAL_HALF_SIZE, 0f);
                glEventMarker.draw(gl, eventsDrawData.eventNames[i], labelYOffset, MAX_GL_VERTICAL_SIZE, drawScale,
                    scaleY);
                gl.glPopMatrix();

                if (i != 0) glEventMarker.getBorders(rect);

                prevX = x;
                prevLabelYOffset = labelYOffset;
            }

            if (isFftProcessing) {
                gl.glMatrixMode(GL10.GL_PROJECTION);
                gl.glLoadIdentity();
                gl.glOrthof(0f, samplesToDraw - 1, 0, MAX_GL_VERTICAL_SIZE, -1f, 1f);

                glFft.draw(gl, fftDrawData);
            }
        }
    }

    //=================================================
    // PRIVATE AND PACKAGE-PRIVATE METHODS
    //=================================================

    @SuppressWarnings("WeakerAccess") void selectWaveform(int index) {
        if (listener != null) listener.onWaveformSelected(index);
    }

    @SuppressWarnings("WeakerAccess") void updateThreshold(float dy) {
        setThreshold(threshold - surfaceHeightToGlHeight(dy) / getWaveformScaleFactor());
    }

    // Returns the color of the waveform for the specified channel in rgba format. If color is not defined green is returned.
    private @Size(4) float[] getWaveformColor(int channel) {
        int counter = 0;
        int channelCount = getChannelCount();
        for (int i = 0; i < channelCount; i++) {
            if (isChannelVisible(i)) {
                if (counter == channel) {
                    channel = i % channelCount;
                    return channelColors[channel];
                }

                counter++;
            }
        }

        return channelColors[channel];
    }
}
