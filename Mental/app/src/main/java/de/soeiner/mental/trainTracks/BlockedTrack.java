package de.soeiner.mental.trainTracks;

/**
 * Created by Malte on 26.04.2016.
 */
public class BlockedTrack extends TrainTrack {

    public BlockedTrack(int value){
        this.setValue(value);
    }

    public int getFrom() {
        return 0;
    }

    @Override
    public int getTo() {
        return 0;
    }

    @Override
    public int getSwitchTo() {
        return 0;
    }

    @Override
    public String getType() {
        return null;
    }
}