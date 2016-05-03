package com.example.tangoserialapplication;

/**
 * Created by brandonsladek on 5/2/16.
 */

public class DepthPoint {

    float x;
    float y;
    float z;

    public DepthPoint(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getDepthPointAsString() {
        return x + ", " + y + ", " + z;
    }

}
