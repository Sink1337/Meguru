package dev.meguru.utils.animations.impl;

import dev.meguru.utils.animations.Animation;
import dev.meguru.utils.animations.Direction;

public class EaseOutSine extends Animation {


    public EaseOutSine(int ms, double endPoint) {
        super(ms, endPoint);
    }

    public EaseOutSine(int ms, double endPoint, Direction direction) {
        super(ms, endPoint, direction);
    }

    @Override
    protected boolean correctOutput() {
        return true;
    }

    @Override
    protected double getEquation(double x) {
        return Math.sin(x * (Math.PI / 2));
    }
}
