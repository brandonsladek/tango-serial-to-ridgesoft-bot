package com.example.tangoserialapplication;

import com.google.atap.tangoservice.TangoPoseData;

/**
 * Created by brandonsladek on 3/30/16.
 */

public class NavigationLogic {

    private double[] hardcodedLocation = new double[]{-3.0, 4.0, 1.0};
    private double[] targetLocation;

    // Constructor
    public NavigationLogic() {}

    public NavigationLogic(double[] targetLocation) {
        this.targetLocation = targetLocation;
    }

    public double getDistance(double[] ourLocation, double[] goLocation) {

        double xDiff = Math.abs(ourLocation[0] - goLocation[0]);
        double yDiff = Math.abs(ourLocation[1] - goLocation[1]);

        double sumOfSquares = Math.pow(xDiff, 2) + Math.pow(yDiff, 2);

        double distance = Math.sqrt(sumOfSquares);

        return distance;
    }

    public char navigate(TangoPoseData poseData, double[] targetLocation) {

        double[] ourLocation = poseData.translation;
        double ourRotation = (int) getPoseRotationDegrees(poseData);

        double ourX = ourLocation[0];
        double ourY = ourLocation[1];
        double ourZ = ourLocation[2];

        float radiusDiff = 2.0f;
        float xDiff = (float) (radiusDiff * Math.cos (45.0));
        float yDiff = (float) (radiusDiff * Math.sin (45.0));

        double[] north = new double[]{ourX, ourY + radiusDiff, ourZ};
        double[] northEast = new double[]{ourX + xDiff, ourY + yDiff, ourZ};
        double[] east = new double[]{ourX + radiusDiff, ourY, ourZ};
        double[] southEast = new double[]{ourX + xDiff, ourY - yDiff, ourZ};
        double[] south = new double[]{ourX, ourY - radiusDiff, ourZ};
        double[] southWest = new double[]{ourX - xDiff, ourY - yDiff, ourZ};
        double[] west = new double[]{ourX - radiusDiff, ourY, ourZ};
        double[] northWest = new double[]{ourX - xDiff, ourY + yDiff, ourZ};

        double[] distances = new double[8];

        double distanceNorth = getDistance(north, targetLocation);
        double distanceNorthEast = getDistance(northEast, targetLocation);
        double distanceEast = getDistance(east, targetLocation);
        double distanceSouthEast = getDistance(southEast, targetLocation);
        double distanceSouth = getDistance(south, targetLocation);
        double distanceSouthWest = getDistance(southWest, targetLocation);
        double distanceWest = getDistance(west, targetLocation);
        double distanceNorthWest = getDistance(northWest, targetLocation);

        distances [0] = distanceNorth;
        distances [1] = distanceNorthEast;
        distances [2] = distanceEast;
        distances [3] = distanceSouthEast;
        distances [4] = distanceSouth;
        distances [5] = distanceSouthWest;
        distances [6] = distanceWest;
        distances [7] = distanceNorthWest;

        int indexWithMinimumDistance = 8;
        double minimumDistance = Double.MAX_VALUE;

        for (int i = 0; i < distances.length; i++) {
            if (distances [i] < minimumDistance) {
                indexWithMinimumDistance = i;
                minimumDistance = distances[i];
            }
        }

        double goRotation = convertIndexToYRotationValue (indexWithMinimumDistance);
        double currentDistance = getDistance(ourLocation, targetLocation);

        return outputDirectionCommand(ourRotation, goRotation, currentDistance);
    }

    public double convertIndexToYRotationValue(int index) {
        switch (index) {
            case 0:
                return 0;
            case 1:
                return 45;
            case 2:
                return 90;
            case 3:
                return 135;
            case 4:
                return 180;
            case 5:
                return 225;
            case 6:
                return 270;
            case 7:
                return 315;
            default:
                return 0;
        }
    }

    public char outputDirectionCommand(double ourRotation, double goRotation, double distance) {

        if (distance < 0.5) {
            // Tell the robot to stop
            return 's';
        } else {
            if (ourRotation < goRotation + 15 && ourRotation > goRotation - 15) {
                // Tell the robot to go forward
                return 'f';
            } else {
                if (ourRotation < goRotation) {
                    double diff = goRotation - ourRotation;
                    if (diff <= 180) {
                        // Tell the robot to turn right
                        return 'r';
                    } else {
                        // Tell the robot to turn left
                        return 'l';
                    }
                } else {
                    double diff = ourRotation - goRotation;
                    if (diff <= 180) {
                        return 'l';
                    } else {
                        return 'r';
                    }
                }
            }
        }
    }

    public float getPoseRotationDegrees(TangoPoseData poseData) {

        float[] translation = poseData.getTranslationAsFloats();
        float x = translation[0];
        float y = translation[1];
        float z = translation[2];
        float w = translation[3];
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

}