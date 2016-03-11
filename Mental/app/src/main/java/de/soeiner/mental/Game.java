package de.soeiner.mental;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by malte on 13.02.16.
 */
public class Game implements Runnable {

    private static ArrayList<Game> games;

    static {
        games = new ArrayList<Game>();
    }

    public static ArrayList<Game> getGames() {
        return games;
    }

    private String name = "";
    private String description = "";
    private ArrayList<Player> joinedPlayers;
    private int difficulty = 0;

    private int result = 0;
    private Score[] scoreboard;

    public Game(String name) {
        games.add(this);
        this.name = name;
        Thread t = new Thread(this);
        t.start();
        joinedPlayers = new ArrayList<Player>();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void destroy() {
        games.remove(this);
    }

    public void updateScoreBoardSize() {
        scoreboard = new Score[joinedPlayers.size()];
        for (int i = 0; i < joinedPlayers.size(); i++) {
            Score s = joinedPlayers.get(i).getScore();
            scoreboard[i] = s;
        }
        broadcastScoreboard();
    }

    public void broadcastScoreboard() {
        for (Player p : joinedPlayers) {
            p.sendScoreBoard(scoreboard);
        }
    }

    public void join(Player p) {
        if (!joinedPlayers.contains(p)) {
            joinedPlayers.add(p);
        }
        p.updateScore();
        updateScoreBoardSize();
    }

    public void leave(Player p) {
        joinedPlayers.remove(p);
        updateScoreBoardSize();
    }

    public void exercise() {

        alreadyRunning = false;
        for (int i = 0; i < joinedPlayers.size(); i++) {
            Player p = joinedPlayers.get(i);
            p.FINISHED = false;
            p.sendExercise(createExercise());
        }

        //der folgende Code schickt allen spielern einen integer (hier 30) um
        // einen countdown starten zu können. Dann wird 30 Sekunden gewartet

        JSONObject j = CmdRequest.makeCmd(CmdRequest.SEND_TIME_LEFT);
        int sekunden = 30;
        try {
            j.put("time", sekunden);
            for (int i = 0; i < joinedPlayers.size(); i++) {
                Player p = joinedPlayers.get(i);
                p.makePushRequest(new PushRequest(j));
            }
            this.wait(sekunden * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (alreadyRunning == false){
            exercise();
        }
    }



    public String createExercise(){

        int temp;
        int a = (int) (Math.random() * 5 * difficulty/2)+1;
        int b = (int) (Math.random() * 5 * difficulty/2)+1;

        if(difficulty % 3 == 0){
            if(a < b){
                temp = a;
                a = b;
                b = temp;
                result = a - b;
                return a+" - "+b;
            }
        }else{
            if(difficulty % 5 == 0){
                while(a * b > 1000){
                    a = (int) (a/10);
                    b = (int) (b/10);
                }
                result = a * b;
                return a+" * "+b;

            }else {
                result = a + b;
                return a+" + "+b;
            }
        }

        difficulty++;
        return "";
    }

    boolean alreadyRunning;

    public boolean playerAnswered(Player p, int answer) {
        boolean allFinished = true;
        Score s = p.getScore();
        if (answer == result) {
            s.setScoreValue(s.getScoreValue() + getPoints());
            sendExerciseSolvedMessage(p.getName(), getRang());
            if(s.getScoreValue() > 100){
                sendPlayerWon(p.getName());
            }
            p.getName();
            p.FINISHED = true;
            for(int i = 0;i < joinedPlayers.size();i++){
                p = joinedPlayers.get(i);
                if(p.FINISHED == false){
                    allFinished = false;
                }
            }
            if(allFinished == true){
                alreadyRunning = true;
                exercise();
            }
            broadcastScoreboard();
            return true;
        } else {
            if(s.getScoreValue() > 0) {
                s.setScoreValue(s.getScoreValue() - 1);
                broadcastScoreboard();
            }
            return false;
        }
    }

    private int getPoints(){ //methode berechent punkte fürs lösen einer Aufgabe
    //jenachdem als wievielter der jeweilige spieler die richtige Antwort eraten hat
        int points = difficulty;
            for(int i = 0; i<getRang();i++){
                points = points/2;
            }
        return points;
    }

    private int getRang(){ //methode berechnet wie viele
    // Spieler die Aufgabe schon gelöst haben
        int rang = 0;
        for(int i = 0; i<joinedPlayers.size();i++){
            Player p = joinedPlayers.get(i);
            if(p.FINISHED == true){
                rang++;
            }
        }
        return rang;
    }
    public void sendExerciseSolvedMessage(String playerName, int rang) {
        String m = playerName+" hat die Aufgabe als "+(rang+1)+". gelöst!";
        for (int i = 0; i < joinedPlayers.size(); i++) {
            Player p = joinedPlayers.get(i);
            JSONObject j = CmdRequest.makeCmd(CmdRequest.SEND_EXERCISE_SOLVED_MESSAGE);
            try {
                j.put("message", m);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            p.makePushRequest(new PushRequest(j));
        }
    }

    public void sendPlayerWon(String playerName) { //wird nur aufgerufen wenn Spieler das Spiel gewonnen hat
        //dem scoreboard können nun auch der zweite und dritte platz entnommen werden
        for (int i = 0; i < joinedPlayers.size(); i++) {
            Player p = joinedPlayers.get(i);

            JSONObject j = CmdRequest.makeCmd(CmdRequest.SEND_PLAYER_WON);
            try {
                j.put("playerName", playerName);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            p.makePushRequest(new PushRequest(j));
        }
    }

    @Override
    public void run() {
        exercise();
    }
}
