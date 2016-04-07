package com.example.tangoserialapplication;

/**
 * Created by brandonsladek on 4/7/16.
 */

public class PoseUtils {

    private MainActivity mainActivity;
    float x;
    float y;
    float z;
    float w;

    public PoseUtils(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public float getPoseRotationDegrees() {

        float t = y*x+z*w;
        int pole;
        float rollRadians;

        if (t > 0.499f) {
            pole = 1;
        } else if (t < -0.499f) {
            pole = -1;
        } else {
            pole = 0;
        }

        if (pole == 0) {
            rollRadians = (float) Math.atan2(2f*(w*z + y*x), 1f - 2f * (x*x + z*z));
        } else {
            rollRadians = pole * 2f * (float) Math.atan2(y, w);
        }

        // 0 - 360
        return (float) Math.toDegrees(rollRadians) + 180;
    }

    public double round(double number) {

        double newNumber = number * 100;
        int newInt = (int) newNumber;

        return newInt / 100.0;
    }

}
