
function Vector(x, y) {
    this.multiply = function(skalar) {
        x *= skalar;
        y *= skalar;
    }
    
    this.add = function(u) {
        x += u.getX();
        y += u.getY();
    }
    
    this.normalize = function() {
        this.multiply(1 / length());
    }
    
    var length = this.length = function() {
        return Math.sqrt(x*x + y*y);
    }
    
    this.getX = function(){return x;};
    this.getY = function(){return y;};
    
    this.copy = function() {
        return new Vector(x, y);
    }
}
// static
Vector.newFromTo = function(u, v) {
    u = u.copy();
    v = v.copy();
    u.multiply(-1);
    v.add(u);
    return v;
}

// used to describe one element of a movement
function Position(x_, y_, r_) {
    var x = this.x = x_;
    var y = this.y = y_;
    var r = this.rotation = r_;
    
    // only move this position relative to the parameter position
    this.move = function(vector) {
        this.x = x = x + vector.getX();
        this.y = y = y + vector.getY();
    }
    
    this.toString = function() {
        return "Pos("+x+","+y+", "+r+")\n";
    }
    
    this.copy = function(vector) {
        if (vector == undefined) vector = new Vector(0,0);
        return new Position(Math.floor(x+vector.getX()),Math.floor(y+vector.getY()),r);
    }
}

// calculates the amount of frames to be rendered in the "time" (in seconds)
function calculateFrameAmount(time) {
    return time * trainGame.graphics.getCurrentFPS();
}

function StraightMovement(rotation, vector, time) {
    var steps = [];
    
    var dx = vector.getX();
    var dy = vector.getY();
    
    var frames = calculateFrameAmount(time);
    
    var x = 0, 
        y = 0, 
        p;
    for (var f = 0; f < frames; f++) {
        x += dx / frames;
        y += dy / frames;
        p = new Position(x, y, rotation);
        steps.push(p);
    }
    
    Movement.call(this, steps);
}
StraightMovement.prototype = new Movement;
StraightMovement.prototype.constructor = StraightMovement;

/**
 * @param degrees in radian - for left, + for right turn
 */
function TurnMovement(startRotation, radius, degrees, time) {
    var steps = [];
    
    var frames = calculateFrameAmount(time);
    var stepWide = -degrees / frames;
    var x, y, p;
    var r = startRotation;
    for (var f = 0; f < frames; f++) {
        r += stepWide;
        if (degrees < 0) {
            x = Math.cos(r) * radius - Math.cos(startRotation) * radius;
        } else {
            x = - Math.cos(r) * radius + Math.cos(startRotation) * radius;
        }
        if (degrees < 0) {
            y = Math.sin(r) * radius - Math.sin(startRotation) * radius;
        } else {
            y = - Math.sin(r) * radius + Math.sin(startRotation) * radius;
        }
        p = new Position(x, y, r);
        steps.push(p);
    }
    
    Movement.call(this, steps);
}
TurnMovement.prototype = new Movement;
TurnMovement.prototype.constructor = TurnMovement;


function Movement(steps) {
    if (steps == undefined) steps = [];
    
    this.getSteps = function() {
        return steps;
    }
    
    var addVector = this.addVector = function(vector) {
        steps = copyTo(vector).getSteps();
    }
    
    var copyTo = this.copyTo = function(vector) {
        var movedSteps = [];
        for (var i = 0; i < steps.length; i++) {
            //alert(steps[i]);
            movedSteps[i] = steps[i].copy(vector);
            //alert(movedSteps[i]);
        }
        return new Movement(movedSteps);
    }
    
    this.getFirst = function() {
        return steps[0];
    }
    
    // between 0 and 1
    this.setProgress = function(p) {
        var newFirstIndex = Math.floor(steps.length * p);
        steps = steps.splice(newFirstIndex);
    }
}

// static Movement class part
Movement.sinValues = [];
Movement.cosValues = [];
Movement.rotationResolution = 250;
Movement.isSetUp = false;

Movement.sin = function(x) {
    if (!Movement.isSetUp) Movement.setup();
    var i = Math.floor(x * Movement.rotationResolution / Math.PI);
    return Movement.sinValues[i];
}

Movement.cos = function(x) {
    if (!Movement.isSetUp) Movement.setup();
    var i = Math.floor(x * Movement.rotationResolution / Math.PI);
    return Movement.cosValues[i];
}

Movement.setup = function() {
   for (var i = 0; i <= Math.PI*2; i+=Math.PI/Movement.rotationResolution) {
       Movement.sinValues.push(Math.sin(i));
       Movement.cosValues.push(Math.cos(i));
   } 
   Movement.isSetUp = true;
}

