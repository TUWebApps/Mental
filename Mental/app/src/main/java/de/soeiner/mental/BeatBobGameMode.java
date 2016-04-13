package de.soeiner.mental;

/**
 * Created by Malte on 09.04.2016.
 */
public class BeatBobGameMode extends GameMode {

    int bobSolveTime;
    int bobStartSolveTime;
    int playerHeadstart;
    int health;
    int status = 0;

    public BeatBobGameMode(Game g){
        super(g);
    }

    public void prepareGame() {
        super.prepareGame();
        status = 0;
        game.individualExercises = true;
        for(int i = 0; i<game.joinedPlayers.size();i++){
            Player p = game.joinedPlayers.get(i);
            game.activePlayers.add(p);
        }
        for (int i = 0; i < game.activePlayers.size(); i++) {
            if(game.exerciseCreator instanceof SimpleMultExerciseCreator) {
                game.activePlayers.get(i).exerciseCreator = new SimpleMultExerciseCreator();
            }else if (game.exerciseCreator instanceof MultExerciseCreator) {
                game.activePlayers.get(i).exerciseCreator = new MultExerciseCreator();
            } else if (game.exerciseCreator instanceof MixedExerciseCreator) {
                game.activePlayers.get(i).exerciseCreator = new MixedExerciseCreator();
            }else{
                game.activePlayers.get(i).exerciseCreator = new SimpleMultExerciseCreator();
            }
            game.activePlayers.get(i).exerciseCreator.setDifficulty(10);
        }
        if(game.activePlayers.size() != 0) {
            bobSolveTime = 4 / game.activePlayers.size()+2; //angenommen ein Spieler benötigt 10 sekunden um eine Aufgabe zu lösen
            bobStartSolveTime = bobSolveTime;
            health = 5 * game.activePlayers.size();
            playerHeadstart = 5;
        }else{
            gameIsRunning = false;
        }
    }

    public void loop() {
        for (int i = 0; i < game.activePlayers.size(); i++) {
            Player player = game.activePlayers.get(i);
            player.sendExercise(player.exerciseCreator.createNext());
        }
        while(getGameIsRunning()){
            try {
                Thread.sleep(playerHeadstart * 1000);
                while(gameIsRunning){
                    balanceBob();
                    Thread.sleep(bobSolveTime * 1000);
                    updateStatus(-1);
                }
            }catch(Exception e){e.printStackTrace();}
        }
    }

    public boolean playerAnswered(Player player, int answer) {

        Score s = player.getScore();
        synchronized (answerLock) {
            if (player.exerciseCreator.checkAnswer(answer)) {
                s.updateScore(5);
                player.exerciseCreator.createNext();
                player.sendExercise(player.exerciseCreator.getExerciseString());
                updateStatus(1);
                game.broadcastMessage(player.getName() + " hat einen Punkt gewonnen");
                answerLock.notify();
                game.broadcastScoreboard();
                return true;
            } else {
                if (s.getScoreValue() > 0) {
                    s.updateScore(-1);
                    game.broadcastScoreboard();
                }
                return false;
            }
        }
    }

    private void updateStatus(int plus){
        status += plus;
        broadcastStatus();
        checkObjective();
    }

    public void checkObjective(){
        if (status >= health) { //wenn bob tot ist
            game.individualExercises = false;
            gameIsRunning = false; // schleife in run() beenden
            game.broadcastPlayerWon("die Spieler", getGameModeString());
            answerLock.notify();
        }
        if(status <= -health){ //wenn spieler tot sind
            game.individualExercises = false;
            gameIsRunning = false; // schleife in run() beenden
            game.broadcastMessage("Bob hat gewonnen");
            game.broadcastPlayerWon("Bob", getGameModeString());
            answerLock.notify();
        }
    }


    public String getGameModeString() {
        return "beatBob";
    }

    public void broadcastStatus(){
        for(int i = 0; i<game.joinedPlayers.size();i++){
            Player p = game.joinedPlayers.get(i);
            p.sendStatus(status);
        }
    }

    public int getIndex(Player p){ //gibt den index eines spielers in der aktiven liste zurück

        for (int i = 0; i < game.activePlayers.size(); i++) {
            if(game.activePlayers.get(i).equals(p)){
                return i;
            }
        }
        return -1;
    }

    public void exerciseTimeout() {}

    private void balanceBob(){
        if(status < 0) {
            if(status >= -game.activePlayers.size()){ // Alles im Normalbereich
                bobSolveTime = bobStartSolveTime;
            }
            if (Math.abs(health) - Math.abs(status) <= game.activePlayers.size()) { // player sind am Arsch
                bobSolveTime *= 2;
            }
        }else if(status > 0){
            if(status <= game.activePlayers.size()){ // Alles im Normalbereich
                bobSolveTime = bobStartSolveTime;
            }
            if (health - status < game.activePlayers.size()){ // Bob ist am Arsch
                bobSolveTime /= 2;
            }
        }
    }
}
