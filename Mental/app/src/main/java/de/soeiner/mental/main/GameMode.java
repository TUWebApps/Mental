package de.soeiner.mental.main;

import org.json.JSONObject;

import java.util.ArrayList;

import de.soeiner.mental.communication.CmdRequest;
import de.soeiner.mental.communication.PushRequest;
import de.soeiner.mental.exerciseCreators.ExerciseCreator;
import de.soeiner.mental.exerciseCreators.MixedExerciseCreator;
import de.soeiner.mental.exerciseCreators.MultExerciseCreator;
import de.soeiner.mental.exerciseCreators.SimpleMultExerciseCreator;
import de.soeiner.mental.trainGame.events.BooleanEvent;
import de.soeiner.mental.util.event.EventDispatcher;
import de.soeiner.mental.util.event.RunState;

/**
 * Created by Malte on 07.04.2016.
 */
public abstract class GameMode {

    public ArrayList<ExerciseCreator> compatibleExerciseCreators = new ArrayList<>();

    private boolean running;
    protected int minPlayers = 2;
    public boolean needsConfirmation = false;
    public Game game;

    public final RunState runState = new RunState();

    public GameMode(Game game) {
        this.game = game;
        initializeCompatibleExerciseCreators();
    }

    public void initializeCompatibleExerciseCreators() {
        compatibleExerciseCreators.add(new MultExerciseCreator());
        compatibleExerciseCreators.add(new MixedExerciseCreator());
        compatibleExerciseCreators.add(new SimpleMultExerciseCreator());
    }

    public void broadcast(JSONObject jsonObject) {
        for (Player player : game.activePlayers) {
            player.makePushRequest(new PushRequest(jsonObject));
        }
    }

    public void waitForPlayers() {
        while (game.joinedPlayers.size() < minPlayers) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            } //Warte auf genügend Spieler
        }
    }

    public Player[] getActivePlayers() {
        return game.activePlayers.toArray(new Player[game.activePlayers.size()]);
    }

    /**
     * main game loop of the GameMode, must block until Game is finished
     */
    public abstract void gameLoop();

    public void prepareGame() {
        resetGameMode();
        game.exerciseCreator.resetDifficulty();
    }

    public void unloadGame() {
        while (game.activePlayers.size() > 0) {
            game.activePlayers.remove(0);
        }
    }


    public abstract boolean playerAction(Player player, JSONObject actionData);

    public abstract String getName();

    public void resetGameMode() {
        running = true;
        for (Player joinedPlayer : game.joinedPlayers) {
            joinedPlayer.getScore().resetScoreValue();
        }
    }

    public ArrayList<ExerciseCreator> getCompatibleExerciseCreators() {
        return compatibleExerciseCreators;
    }


    public void newExercise() {
        game.exerciseCreator.next(); // erstellt neue aufgabe
        game.broadcastExercise(); // sendet aufgabe an alle spieler
        game.exerciseCreator.increaseDifficulty();
    }

    public void removePlayer(Player p) {}

    public void addAllPlayersToActive(){
        for (int i = 0; i < game.joinedPlayers.size(); i++) {
            game.activePlayers.add(game.joinedPlayers.get(i));
        }
    }

    public void openGUIFrame() {  // TODO make more flexible - cooperate with JS Frames
        for (int i = 0; i < game.joinedPlayers.size(); i++) {
            Player p = game.joinedPlayers.get(i);
            try {
                JSONObject j = CmdRequest.makeCmd(CmdRequest.SHOW_EXERCISES);
                j.put("exerciseType", game.exerciseCreator.getType());
                p.makePushRequest(new PushRequest(j));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}