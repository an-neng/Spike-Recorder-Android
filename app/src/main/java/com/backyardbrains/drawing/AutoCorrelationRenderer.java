package com.backyardbrains.drawing;

import android.content.Context;
import android.support.annotation.NonNull;
import com.backyardbrains.drawing.gl.GlBarGraph;
import com.backyardbrains.drawing.gl.GlBarGraphThumb;
import com.backyardbrains.drawing.gl.GlGraphThumbTouchHelper.Rect;
import com.backyardbrains.ui.BaseFragment;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class AutoCorrelationRenderer extends BaseAnalysisRenderer {

    static final String TAG = makeLogTag(AutoCorrelationRenderer.class);

    private static final float[] H_GRAPH_AXIS_VALUES = new float[6];

    static {
        H_GRAPH_AXIS_VALUES[0] = 0f;
        H_GRAPH_AXIS_VALUES[1] = .02f;
        H_GRAPH_AXIS_VALUES[2] = .04f;
        H_GRAPH_AXIS_VALUES[3] = .06f;
        H_GRAPH_AXIS_VALUES[4] = .08f;
        H_GRAPH_AXIS_VALUES[5] = .1f;
    }

    private GlBarGraph glBarGraph;
    private GlBarGraphThumb glBarGraphThumb;
    private Context context;

    @SuppressWarnings("WeakerAccess") int[][] autocorrelationAnalysis;

    public AutoCorrelationRenderer(@NonNull BaseFragment fragment) {
        super(fragment);

        context = fragment.getContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        glBarGraph = new GlBarGraph(context, gl);
        glBarGraphThumb = new GlBarGraphThumb(context, gl);
    }

    @Override protected void draw(GL10 gl, int surfaceWidth, int surfaceHeight) {
        if (getAutocorrelationAnalysis()) {
            int len = autocorrelationAnalysis.length;
            if (len > 0) {
                final float thumbSize = getDefaultGraphThumbSize(surfaceWidth, surfaceHeight, len);
                boolean portraitOrientation = surfaceWidth < surfaceHeight;
                float x, y, w, h;
                for (int i = 0; i < len; i++) {
                    x = portraitOrientation ? MARGIN * (i + 1) + thumbSize * i
                        : (float) surfaceWidth - (thumbSize + MARGIN);
                    y = portraitOrientation ? MARGIN : (float) surfaceHeight - (MARGIN * (i + 1) + thumbSize * (i + 1));
                    w = h = thumbSize;
                    // pass thumb to parent class so we can detect thumb click
                    glGraphThumbTouchHelper.registerTouchableArea(new Rect(x, y, thumbSize, thumbSize));
                    glBarGraphThumb.draw(gl, x, y, w, h, autocorrelationAnalysis[i],
                        Colors.CHANNEL_COLORS[i % Colors.CHANNEL_COLORS.length],
                        SPIKE_TRAIN_THUMB_GRAPH_NAME_PREFIX + (i + 1));
                }
                x = MARGIN;
                y = portraitOrientation ? 2 * MARGIN + thumbSize : MARGIN;
                w = portraitOrientation ? surfaceWidth - 2 * MARGIN : surfaceWidth - 3 * MARGIN - thumbSize;
                h = portraitOrientation ? surfaceHeight - 3 * MARGIN - thumbSize : surfaceHeight - 2 * MARGIN;

                int selected = glGraphThumbTouchHelper.getSelectedTouchableArea();
                glBarGraph.draw(gl, x, y, w, h, autocorrelationAnalysis[selected], H_GRAPH_AXIS_VALUES,
                    Colors.CHANNEL_COLORS[selected % Colors.CHANNEL_COLORS.length]);
            }
        }
    }

    //=================================================
    //  PRIVATE METHODS
    //=================================================

    private boolean getAutocorrelationAnalysis() {
        if (autocorrelationAnalysis != null && autocorrelationAnalysis.length > 0) return true;

        if (getAnalysisManager() != null) {
            autocorrelationAnalysis = getAnalysisManager().getAutocorrelation();
            if (autocorrelationAnalysis != null) {
                LOGD(TAG, "AUTOCORRELATION ANALYSIS RETURNED: " + autocorrelationAnalysis.length);
            }
            return autocorrelationAnalysis != null;
        }

        return false;
    }
}
