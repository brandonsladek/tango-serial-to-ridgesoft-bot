package com.example.tangoserialapplication;

import java.io.Serializable;

/**
 * Created by brandonsladek on 4/26/16.
 */

public class SafePoint implements Serializable {

    private double x;
    private double y;
    private double z;

    public SafePoint(double[] location) {
        this.x = location[0];
        this.y = location[1];
        this.z = location[2];
    }

    public double[] getPoint() {
        return new double[]{x, y, z};
    }

    public String getPointAsString() {
        return x + ", " + y + ", " + z;
    }
}
