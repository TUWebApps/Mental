package de.soeiner.mental;

/**
 * Created by Malte on 28.03.2016.
 */

public class Classic extends Game{

    static String gameMode = "Classic";

    public Classic(String name){
        super(name, new MixedExerciseCreator2(1)); //mit leichtestem schwierigkeitsgrad
    }

    public void run() {
        start:
        while(true) {
            exerciseCreator.resetDifficulty();
            while (joinedPlayers.size() == 0) { //Warten bis spieler das Spiel betreten hat
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
            gameIsLive = true;

            while (gameIsLive) {
                if (joinedPlayers.size() == 0) { //wenn keine spieler mehr da sind
                    continue start; //springe zurück in den Wartezustand
                } else {
                    broadcastExercise();
                    exerciseCreator.increaseDifficulty();
                    synchronized (this) { // ist angefordert damit man wait oder notify nutzen kann
                        try {
                            wait(EXERCISE_TIMEOUT * 1000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            try { //Zeit für einen siegerbildschrim mit erster,zweiter,dritter platz ?
                sendScoreStrings();
                Thread.sleep(GAME_TIMEOUT * 1000);
            } catch (InterruptedException e) {
            }

            // punktestaende fuer alle Spieler zuruecksetzen
            for (int i = 0; i < joinedPlayers.size(); i++) {
                Player p = joinedPlayers.get(i);
                p.getScore().resetScoreValue(); //reset
            }
        }
    }

        public boolean playerAnswered(Player player, int answer) {
            boolean allFinished = true;
            Score s = player.getScore();
            synchronized (this) {
                if(!player.finished) { // sonst kann man 2x mal punkte absahnen ;; spieler kriegt jetzt keine punkte mehr abgezogen für doppeltes antworten
                    if (exerciseCreator.checkAnswer(answer)) {
                        s.updateScore(getPoints());
                        broadcastMessage(player.getName()+" hat die Aufgabe als "+(getRank()+1)+". gelöst!");
                        if (s.getScoreValue() > 100) {
                            broadcastPlayerWon(player.getName(), gameMode);
                            notify(); // hat einer gewonnen, muss das wait im game loop ebenfalls beendet werden.
                        }
                        player.finished = true;
                        for (int i = 0; i < joinedPlayers.size(); i++) {
                            Player p = joinedPlayers.get(i);
                            if (!p.finished) {
                                allFinished = false;
                            }
                        }
                        if (allFinished) {
                            notify(); // beendet das wait in loop() vorzeitig wenn alle fertig sind
                        }
                        broadcastScoreboard();
                        return true;
                    } else {
                        if (s.getScoreValue() > 0) {
                            s.updateScore(-1);
                            broadcastScoreboard();
                        }
                        return false;
                    }
                }
                return true;
            }
        }
    }

