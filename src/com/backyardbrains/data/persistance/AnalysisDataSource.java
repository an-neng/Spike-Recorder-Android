package com.backyardbrains.data.persistance;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.SpikeValueAndIndex;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.data.persistance.entity.Train;
import com.backyardbrains.utils.ThresholdOrientation;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
public interface AnalysisDataSource {

    interface GetAnalysisCallback<T> {
        void onAnalysisLoaded(@NonNull T result);

        void onDataNotAvailable();
    }

    //=================================================
    //  SPIKE ANALYSIS
    //=================================================

    interface SpikeAnalysisCheckCallback {
        void onSpikeAnalysisExistsResult(boolean exists);
    }

    void spikeAnalysisExists(@NonNull String filePath, @Nullable SpikeAnalysisCheckCallback callback);

    void saveSpikeAnalysis(@NonNull String filePath, @NonNull Spike[] spikesAnalysis);

    void getSpikeAnalysis(@NonNull String filePath, @Nullable GetAnalysisCallback<Spike[]> callback);

    SpikeValueAndIndex[] getSpikesAnalysisTimesAndIndicesByTrainForRange(long trainId, int startIndex, int endIndex);

    void getSpikeAnalysisTimesByTrains(@NonNull final String filePath,
        @Nullable final GetAnalysisCallback<float[][]> callback);

    void getSpikeAnalysisIndicesByTrains(@NonNull String filePath, @Nullable GetAnalysisCallback<int[][]> callback);

    //=================================================
    //  SPIKE TRAINS
    //=================================================

    interface AddSpikeAnalysisTrainCallback {
        void onSpikeAnalysisTrainAdded(@NonNull Train train);
    }

    interface RemoveSpikeAnalysisTrainCallback {
        void onSpikeAnalysisTrainRemoved(int newTrainCount);
    }

    void getSpikeAnalysisTrains(@NonNull String filePath, @Nullable GetAnalysisCallback<Train[]> callback);

    void addSpikeAnalysisTrain(@NonNull String filePath, @Nullable AddSpikeAnalysisTrainCallback callback);

    void saveSpikeAnalysisTrain(@NonNull String filePath, @ThresholdOrientation int orientation, int value, int order);

    void removeSpikeAnalysisTrain(@NonNull String filePath, int trainOrder,
        @Nullable RemoveSpikeAnalysisTrainCallback callback);
}
