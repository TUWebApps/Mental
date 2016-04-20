
var mainFrame = new Frame("mainFrame");

mainFrame.setOnOpen(function() {
    byID("answer").focus();
    setDoOnEnter(function(){sendAnswer();});
    byID("disconnect").style.display = "none";
    byID("toLobby").style.display = "inline";
    byID("messageContainerDivision").style.opacity = 1;
    serverConnection.addObserver(beatBobObserver);
    serverConnection.addObserver(messageObserver);
    serverConnection.addObserver(exerciseObserver);
    serverConnection.addObserver(playerWonObserver);
    resetBeatBobBar();
});

mainFrame.setOnClose(function() {
    serverConnection.removeObserver(beatBobObserver);
    serverConnection.removeObserver(messageObserver);
    serverConnection.removeObserver(exerciseObserver);
    serverConnection.removeObserver(playerWonObserver);
    byID("messageContainerDivision").style.opacity = 0;
    byID("beatBob").style.opacity = 0;
    blur();
});



// FUNCTIONALITY =======================================================

function numpad(n) {
    byID("answer").value += String(n);
}

function numpadDel() {
    var v = String(byID("answer").value);
    byID("answer").value = v.substring(0, v.length-1);
}

var alreadyAnswered = false;
function sendAnswer() {
    if (alreadyAnswered) {return;}
    alreadyAnswered = true;
    log(alreadyAnswered);
    setTimeout(function(){alreadyAnswered = false;}, 100); // hier lieber ein Timeout, da es ja sein kann, dass keine Antwort vom Server kommt (dann waere diese Methode für immer gelockt!)
    var answer = byID("answer").value;
    var cmdObject = {};
    cmdObject.type = "answer";
    cmdObject.answer = {value:answer};
    serverConnection.communicate(cmdObject, function(msg) {
        if (msg.isCorrect) {
            byID("answer").style.backgroundColor = "#afa";
            byID("answer").placeholder = "Richtig!";
        } else {
            byID("answer").style.backgroundColor = "#faa";
            byID("answer").placeholder = "Falsch!";
            byID("answer").value = ""; // bei einer falschen Antwort wird das ergebnis gelöscht, bei einer richtigen Antwort bleibt das Ergebnis stehen, bis die nächste Aufgabe kommt
        }
        setTimeout(function(){
            byID("answer").style.backgroundColor = "#fff";
        }, 1000);
    });
}

function resetBeatBobBar() {
    byID("beatBobBarLeft").style.width = 0;
    byID("beatBobBarRight").style.width = 0;
    byID("beatBobBarLeft").style.backgroundColor = "#ff0";
    byID("beatBobBarRight").style.backgroundcolor = "#ff0";
}

function showBeatBobBar() {
    
}



// OBSERVERS ===========================================================

var playerWonObserver = new Observer("playerWon", function(msg) {
    countdownValue = Number(msg.gameTimeout);
    countDownId = "gameTimeoutCountdown";
});

var exerciseObserver = new Observer("exercise", function(msg) {
    var ex = msg.exercise;
    byID("exercise").innerHTML = ex + " = ";
    byID("answer").placeholder = "?";
    byID("answer").value = "";
});

var reopenMainFrameObserver = new Observer("showExercises", function(msg) {
    navigation.openFrames(mainFrame);
    countDownId = "exerciseCountdown";
});

var beatBobObserver = new Observer("beatbob", function(msg) {
    var p = Math.abs(msg.status);
    var p_ = 1 - p;
    var percent = 100 * p;
    if (msg.status < 0) {
        byID("beatBobBarLeft").style.width = percent + "%";
        var c = 'rgb('+(155+100*p)+','+(150*p_)+','+(150*p_)+')';
        byID("beatBobBarLeft").style.backgroundColor = c;
        byID("beatBobBarLeft").style.boxShadow = "0px 0px 3px " + c;
    }
    if (msg.status == 0) resetBeatBobBar();
    if (msg.status > 0) {
        byID("beatBobBarRight").style.width = percent + "%";
        var c = 'rgb('+(150*p_)+','+(155+100*p)+','+(150*p_)+')';
        byID("beatBobBarRight").style.backgroundColor = c;
        byID("beatBobBarRight").style.boxShadow = "0px 0px 3px " + c;
    }
});

var messageObserver = new Observer("message", function(msg){displayMessage(msg.message);});
