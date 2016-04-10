package com.example.tangoserialapplication;

import com.google.atap.tangoservice.TangoPoseData;

/**
 * Created by brandonsladek on 3/30/16.
 */

public class NavigationLogic {

    private TangoSerialConnection tsConn;
    private MainActivity mainActivity;

    private double[] hardcodedLocation = new double[]{-3.0, 4.0, 1.0};

    // Constructor
    public NavigationLogic(TangoSerialConnection tsConn, MainActivity mainActivity) {
        this.tsConn = tsConn;
        this.mainActivity = mainActivity;
    }

    public double getDistance(double[] ourLocation, double[] goLocation) {

        double xDiff = Math.abs(ourLocation[0] - goLocation[0]);
        double yDiff = Math.abs(ourLocation[1] - goLocation[1]);

        double sumOfSquares = Math.pow(xDiff, 2) + Math.pow(yDiff, 2);

        double distance = Math.sqrt(sumOfSquares);

        return distance;
    }

    public void navigate(TangoPoseData poseData) {

        double[] ourLocation = poseData.translation;
        double[] rotationInfo = poseData.rotation;
        double ourRotation = rotationInfo[1];

        double ourX = ourLocation[0];
        double ourY = ourLocation[1];
        double ourZ = ourLocation[2];

        float radiusDiff = 2.0f;
        float xDiff = (float) (radiusDiff * Math.cos (45.0));
        float yDiff = (float) (radiusDiff * Math.sin (45.0));

        double[] north = new double[]{ourX, ourY, ourZ + radiusDiff};
        double[] northEast = new double[]{ourX + xDiff, ourY, ourZ + yDiff};
        double[] east = new double[]{ourX + radiusDiff, ourY, ourZ};
        double[] southEast = new double[]{ourX + xDiff, ourY, ourZ - yDiff};
        double[] south = new double[]{ourX, ourY, ourZ - radiusDiff};
        double[] southWest = new double[]{ourX - xDiff, ourY, ourZ - yDiff};
        double[] west = new double[]{ourX - radiusDiff, ourY, ourZ};
        double[] northWest = new double[]{ourX - xDiff, ourY, ourZ + yDiff};

        double[] distances = new double[8];

        double distanceNorth = getDistance(north, hardcodedLocation);
        double distanceNorthEast = getDistance(northEast, hardcodedLocation);
        double distanceEast = getDistance(east, hardcodedLocation);
        double distanceSouthEast = getDistance(southEast, hardcodedLocation);
        double distanceSouth = getDistance(south, hardcodedLocation);
        double distanceSouthWest = getDistance(southWest, hardcodedLocation);
        double distanceWest = getDistance(west, hardcodedLocation);
        double distanceNorthWest = getDistance(northWest, hardcodedLocation);

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
        double currentDistance = getDistance(ourLocation, hardcodedLocation);

        outputDirectionCommand(ourRotation, goRotation, currentDistance);
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

    public void outputDirectionCommand(double ourRotation, double goRotation, double distance) {

        if (distance < 0.5) {
            // Tell the robot to stop
            tsConn.handleMessage('s');
        } else {
            if (ourRotation < goRotation + 15 && ourRotation > goRotation - 15) {
                // Tell the robot to go forward
                tsConn.handleMessage('f');
            } else {
                if (ourRotation < goRotation) {
                    double diff = goRotation - ourRotation;
                    if (diff <= 180) {
                        // Tell the robot to turn right
                        tsConn.handleMessage('r');
                    } else {
                        // Tell the robot to turn left
                        tsConn.handleMessage('l');
                    }
                } else {
                    double diff = ourRotation - goRotation;
                    if (diff <= 180) {
                        tsConn.handleMessage('l');
                    } else {
                        tsConn.handleMessage('r');
                    }
                }
            }
        }
    }

}