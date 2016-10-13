package de.soeiner.mental.trainGame.gameModes;

import de.soeiner.mental.main.Game;
import de.soeiner.mental.trainGame.events.HealthLimitReachedEvent;
import de.soeiner.mental.trainGame.gameConditions.HealthWithRestoreGameCondition;
import de.soeiner.mental.trainGame.trainGenerators.TrainGenerator;
import de.soeiner.mental.trainGame.trainGenerators.TrainWaveGenerator;
import de.soeiner.mental.trainGame.trainGenerators.Wave;
import de.soeiner.mental.trainGame.events.TrainArrivedEvent;
import de.soeiner.mental.trainGame.trainTracks.TrainTrack;
import de.soeiner.mental.util.event.EventListener;
import de.soeiner.mental.util.flow.Blocker;
import de.soeiner.mental.util.flow.EventBlocker;
import de.soeiner.mental.util.flow.TimeoutBlocker;

/**
 * Created by Malte on 21.04.2016.
 */
public abstract class WavesTrainGameMode extends TrainGameMode {

    protected Wave wave;
    private int waveCounter = 1;
    protected Blocker betweenWaveBlocker = new TimeoutBlocker(5000);

    private TrainGenerator trainGenerator;

    public WavesTrainGameMode(final Game game) {
        super(game);
        super.trainArrived.addListenerOnce(trainArrivedListener);
    }

    @Override
    public void prepareGame() {
        super.prepareGame();
        resetWaveCounter();
        wave = getNextWave();
    }

    protected EventListener<HealthLimitReachedEvent> broadcastWaveCompletedListener = new EventListener<HealthLimitReachedEvent>() {
        @Override
        public void onEvent(HealthLimitReachedEvent event) {
            for (int i = 0; i < game.activePlayers.size(); i++) {
                game.activePlayers.get(i).sendWaveCompleted(event.isPositive(), waveCounter++, 0);
            }
        }
    };

    @Override
    public String getName() {
        return "Waves - Coop";
    }

    @Override
    public void gameLoop() {
        countdown(5);
        System.out.println("countdown ended");
        runState.setRunning(true);
        System.out.println("set running to true");
        while (runState.isRunning()) {
            trainGenerator = new TrainWaveGenerator(this, wave, game.activePlayers.size(), getAvailableMatchingIds(), new TrainTrack[]{getTrackById(getFirstTrackId())});
            trainGenerator.runState.setRunning(true);
            HealthWithRestoreGameCondition waveCondition = new HealthWithRestoreGameCondition(this, 10, 0, 20);
            waveCondition.addListener(broadcastWaveCompletedListener);
            new EventBlocker<>(waveCondition.conditionMetOrAborted).block(); // TODO
            trainGenerator.runState.setRunning(false);
            if (hasNextWave()) {
                System.out.println("blocker");
                betweenWaveBlocker.block();
                wave = getNextWave();
                System.out.println("got next wave");
            } else {
                System.out.println("Players lost");
                runState.setRunning(false);
            }
        }
        System.out.println("stopping trainGenerator");
    }

    private EventListener<TrainArrivedEvent> trainArrivedListener = new EventListener<TrainArrivedEvent>() {
        @Override
        public void onEvent(TrainArrivedEvent event) {

        }
    };

    public boolean isWaveRunning() {
        return trainGenerator.runState.isRunning();
    }

    protected abstract boolean hasNextWave();

    protected abstract Wave getNextWave();

    protected abstract void resetWaveCounter();

}
