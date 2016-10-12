package de.soeiner.mental.trainGame.gameModes;

import org.json.JSONObject;

import java.util.ArrayList;

import de.soeiner.mental.exerciseCreators.PathBasedTrainMapCreator;
import de.soeiner.mental.exerciseCreators.PathFinderTrainMapCreator;
import de.soeiner.mental.exerciseCreators.TrainMapCreator;
import de.soeiner.mental.main.Game;
import de.soeiner.mental.main.Player;
import de.soeiner.mental.main.GameMode;
import de.soeiner.mental.trainGame.Train;
import de.soeiner.mental.trainGame.trainGenerators.Wave;
import de.soeiner.mental.trainGame.events.TrainArrivedEvent;
import de.soeiner.mental.trainGame.events.TrainSpawnEvent;
import de.soeiner.mental.trainGame.trainTracks.Goal;
import de.soeiner.mental.trainGame.trainTracks.Switch;
import de.soeiner.mental.trainGame.trainTracks.TrainTrack;
import de.soeiner.mental.util.event.EventDispatcher;

/**
 * Created by Malte on 14.09.2016.
 */
public abstract class TrainGameMode extends GameMode {

    public TrainTrack[][] trainMap;
    Switch[] switches;
    Goal[] goals;
    Wave[] waves;
    Integer[] availableMatchingIds;

    public boolean waveIsRunning;
    public boolean waveSuccess;
    int health;
    int healthNeededToWin;
    int reward;
    int trainArrivedReward;
    protected TrainMapCreator trainMapCreator;

    public EventDispatcher<TrainArrivedEvent> trainArrived = new EventDispatcher<>();
    public EventDispatcher<TrainSpawnEvent> trainSpawn = new EventDispatcher<>();

    public TrainGameMode(Game game) {
        super(game);
        needsConfirmation = true;
    }

    public void initializeCompatibleExerciseCreators() {
        compatibleExerciseCreators.add(new PathBasedTrainMapCreator(game));
        compatibleExerciseCreators.add(new PathFinderTrainMapCreator(game));
    }

    @Override
    public void setRunning(boolean flag) {
        super.setRunning(flag);
        this.waveIsRunning = this.waveSuccess = false;
    }

    abstract Wave[] initiateWaves();

    public void prepareGame() {
        super.prepareGame();
        distributePlayers();
        trainMapCreator = (TrainMapCreator) game.exerciseCreator;
        prepareMapCreation();
        trainMapCreator.next(); // erstellt die neue map
        trainMap = trainMapCreator.getTrainMap();
        switches = getSwitches();
        goals = getGoals();
        getAvailableMatchingIds();
        waves = initiateWaves();
        prepareMap();
        trainMapCreator.updateExerciseObject();
        game.broadcastExercise(); // macht nichts außer die map an alle zu senden
        prepareGameStart();
    }

    //diese mehtoden sind jetzt nicht mehr abstract sonder müssen überschrieben werden

    /**
     * zusätzliches vorbereitungen wie das manuelle setzen der Spieleranzahl
     */
    public void prepareMapCreation() {
    }

    /**
     * zusätzliches vorbereitungen wie die farbgebung der goals
     */
    public void prepareMap() { //
        for (Goal goal : goals) {
            goal.setMatchingId(goal.getGoalId());
        }
    }

    /**
     * zusätzliches Vorbereitungen nach dem senden der map
     */
    public void prepareGameStart() {
        alignSwitchesInGUI();
    }

    /**
     * verteilen der Spieler auf activeplayers oder teams usw
     */
    public void distributePlayers() {
        addAllPlayersToActive();
    }

    protected Switch[] getSwitches() {
        ArrayList<TrainTrack> arrayList = trainMapCreator.scanSurroundings(0, 0, Math.max(trainMap.length, trainMap[0].length), TrainMapCreator.TrainTrackPredicates.containsSwitch);
        game.broadcastMessage("Num Switches: " + arrayList.size());
        return arrayList.toArray(new Switch[arrayList.size()]);
    }

    protected Goal[] getGoals() {
        ArrayList<TrainTrack> arrayList = trainMapCreator.scanSurroundings(0, 0, Math.max(trainMap.length, trainMap[0].length), TrainMapCreator.TrainTrackPredicates.containsGoal);
        game.broadcastMessage("Num Goals: " + arrayList.size());
        return arrayList.toArray(new Goal[arrayList.size()]);
    }

    protected Integer[] getAvailableMatchingIds() {
        ArrayList<Integer> availableMatchingIds = new ArrayList<>();
        for (Goal goal : goals) {
            int id = goal.getMatchingId();
            if (!availableMatchingIds.contains(id)) {
                availableMatchingIds.add(id);
            }
        }
        return this.availableMatchingIds = availableMatchingIds.toArray(new Integer[availableMatchingIds.size()]);
    }

    public TrainTrack getTrackById(int id) {
        for (int i = 0; i < trainMap.length; i++) {
            for (int j = 0; j < trainMap[i].length; j++) {
                if (trainMap[i][j] != null && trainMap[i][j].getId() == id) {
                    return trainMap[i][j];
                }
            }
        }
        throw new RuntimeException("getTrackById(), konnte keine Track mit id" + id + " finden");
    }

    public int getFirstTrackId() {
        return ((TrainMapCreator) game.exerciseCreator).getFirstTrackId();
    }

    protected void giveReward(int reward) {
        for (int i = 0; i < game.activePlayers.size(); i++) {
            game.activePlayers.get(i).getScore().updateScore(reward);
        }
    }

    protected void playersWon() {
        game.broadcastMessage("You Won! Reward: " + reward + "$");
        //game.broadcastMessage("und bekommen einen Bonus von " + reward + "$ !");
        giveReward(reward);
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
        }
    }

    public boolean playerAction(Player player, JSONObject actionData) {
        if (actionData.has("switch")) {
            try {
                for (Switch s : switches) {
                    if (s.getSwitchId() == actionData.getInt("switch")) {
                        s.changeSwitch(actionData.getInt("switchedTo"));
                        broadcastSwitchChange(s);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public void countdown(int from) {
        for (int i = from; i > 0; i--) {
            game.broadcastMessage("" + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        game.broadcastMessage("GO!");
    }

    protected void goThroughWaves() {
        int destinationId = 0;
        int idcounter = 0;
        double speed = 0;
        countdown(5);
        for (int i = 0; i < waves.length && running; i++) {
            health = waves[i].getHealth();
            healthNeededToWin = waves[i].getHealthNeededToWin();
            trainArrivedReward = waves[i].getTrainArrivedReward();
            waveIsRunning = true;
            while (waveIsRunning && running) {
                destinationId = (int) (Math.random() * goals.length); // da die goalId jetzt gleich der values sind und bei 1 starten, muss hier +1 stehen
                speed = Math.random() * (waves[i].getMaxSpeed() - waves[i].getMinSpeed()) + waves[i].getMinSpeed();
                new Train(idcounter, destinationId, speed, this, false); //zug spawnen
                idcounter++;
                try {
                    Thread.sleep(waves[i].getTrainSpawnInterval()); //warten
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (waveSuccess) {
                giveReward(waves[i].getReward());
                broadcastWaveCompleted(true, i, waves[i].getReward());
                System.out.println("welle " + i + " erfolgreich abgeschlossen!");
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                broadcastWaveCompleted(false, i, waves[i].getReward());
                running = false;
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            if (i == waves.length - 1) {
                playersWon();
                running = false;
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Goal findGoalById(int id) {
        for (int i = 0; i < goals.length; i++) {
            if (goals[i].getGoalId() == id) return goals[i];
        }
        throw new Error("Goal nicht gefunden");
    }

    public void alignSwitchesInGUI() {
        for (int i = 0; i < switches.length; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            broadcastSwitchChange(switches[i]);
        }
    }

    public void broadcastSwitchChange(Switch s) {
        for (int i = 0; i < game.activePlayers.size(); i++) {
            game.activePlayers.get(i).sendSwitchChange(s);
        }
    }

    public void broadcastNewTrain(JSONObject train) {
        for (int i = 0; i < game.activePlayers.size(); i++) {
            game.activePlayers.get(i).sendNewTrain(train);
        }
    }

    public void broadcastTrainDecision(int trainId, int switchId, int direction) {
        for (int i = 0; i < game.activePlayers.size(); i++) {
            game.activePlayers.get(i).sendTrainDecision(trainId, switchId, direction);
        }
    }

    public void broadcastWaveCompleted(boolean success, int waveNo, int reward) {
        for (int i = 0; i < game.activePlayers.size(); i++) {
            game.activePlayers.get(i).sendWaveCompleted(success, (waveNo + 1), reward);
        }
    }

    //@Override
    public void newExercise() {
    }
}
