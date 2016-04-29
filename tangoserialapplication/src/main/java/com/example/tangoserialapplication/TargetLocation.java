package com.example.tangoserialapplication;

/**
 * Created by brandonsladek on 4/28/16.
 */

public class TargetLocation {

    private double x;
    private double y;
    private double z;
    private int rotation;

    public TargetLocation(double[] location, int rotation) {
        this.x = location[0];
        this.y = location[1];
        this.z = location[2];
        this.rotation = rotation;
    }

    public double[] getTargetLocation() {
        return new double[]{x, y, z};
    }

    public int getRotation() {
        return rotation;
    }

    public String getTargetLocationAsString() {
        return x + ", " + y + ", " + z + ", Rotation: " + rotation;
    }

}
