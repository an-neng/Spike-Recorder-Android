package com.backyardbrains.data.persistance.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.data.persistance.entity.SpikeTrain;
import java.util.List;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
@Dao public interface SpikeTrainDao {

    @Insert void insertSpikeTrains(List<SpikeTrain> spikeTrains);

    @Query("SELECT id, analysis_id, value, `index`, time FROM spikes INNER JOIN spike_trains ON spikes.id = spike_trains.spike_id WHERE spike_trains.train_id = :trainId ORDER BY spikes.`index`")
    Spike[] loadSpikes(long trainId);

    @Query("SELECT time FROM spikes INNER JOIN spike_trains ON spikes.id = spike_trains.spike_id WHERE spike_trains.train_id = :trainId ORDER BY spikes.`index`")
    float[] loadSpikeTimes(long trainId);

    @Query("SELECT `index` FROM spikes INNER JOIN spike_trains ON spikes.id = spike_trains.spike_id WHERE spike_trains.train_id = :trainId ORDER BY spikes.`index`")
    int[] loadSpikeIndices(long trainId);
}