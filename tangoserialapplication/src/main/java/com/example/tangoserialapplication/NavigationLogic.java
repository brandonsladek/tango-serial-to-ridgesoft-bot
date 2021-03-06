package com.example.tangoserialapplication;

import java.io.Serializable;

/**
 * Created by brandonsladek on 3/30/16.
 */

public class NavigationLogic implements Serializable {

    public NavigationInfo navigationInfo(double[] translation, double[] rotation, double[] target) {

        double ourX = translation[0];
        double ourY = translation[1];
        double ourZ = translation[2];

        double radiusDiff = 1.0;
        double xDiff = radiusDiff * Math.cos (45.0);
        double yDiff = radiusDiff * Math.sin (45.0);

        double[] north = new double[]{ourX, ourY - radiusDiff, ourZ};
        double[] northWest = new double[]{ourX + xDiff, ourY - yDiff, ourZ};
        double[] west = new double[]{ourX + radiusDiff, ourY, ourZ};
        double[] southWest = new double[]{ourX + xDiff, ourY + yDiff, ourZ};
        double[] south = new double[]{ourX, ourY + radiusDiff, ourZ};
        double[] southEast = new double[]{ourX - xDiff, ourY + yDiff, ourZ};
        double[] east = new double[]{ourX - radiusDiff, ourY, ourZ};
        double[] northEast = new double[]{ourX - xDiff, ourY - yDiff, ourZ};

        double[] distances = new double[8];

        double distanceNorth = getDistance(north, target);
        double distanceNorthEast = getDistance(northEast, target);
        double distanceEast = getDistance(east, target);
        double distanceSouthEast = getDistance(southEast, target);
        double distanceSouth = getDistance(south, target);
        double distanceSouthWest = getDistance(southWest, target);
        double distanceWest = getDistance(west, target);
        double distanceNorthWest = getDistance(northWest, target);

        distances [0] = distanceNorth;
        distances [1] = distanceNorthWest;
        distances [2] = distanceWest;
        distances [3] = distanceSouthWest;
        distances [4] = distanceSouth;
        distances [5] = distanceSouthEast;
        distances [6] = distanceEast;
        distances [7] = distanceNorthEast;

        int indexWithMinimumDistance = 8;
        double minimumDistance = Double.MAX_VALUE;

        for (int i = 0; i < distances.length; i++) {
            if (distances [i] < minimumDistance) {
                indexWithMinimumDistance = i;
                minimumDistance = distances[i];
            }
        }

        int ourRotation = (int) getOurRotation(rotation);
        int goRotation = convertIndexToYRotationValue(indexWithMinimumDistance);
        double currentDistance = getDistance(translation, target);

        char command = getDirectionCommand(ourRotation, goRotation, currentDistance);

        NavigationInfo navigationInfo = new NavigationInfo(ourRotation, goRotation, command);

        return navigationInfo;
    }

    public double getDistance(double[] ourLocation, double[] goLocation) {

        double xDiff = Math.abs(ourLocation[0] - goLocation[0]);
        double yDiff = Math.abs(ourLocation[1] - goLocation[1]);

        double sumOfSquares = Math.pow(xDiff, 2) + Math.pow(yDiff, 2);

        return Math.sqrt(sumOfSquares);
    }

    private int convertIndexToYRotationValue(int index) {

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

    private char getDirectionCommand(double ourRotation, double goRotation, double distance) {

        if (distance < 0.025) {
            // Tell the robot to stop
            return 's';
        } else {
            if (ourRotation < goRotation + 22.5 && ourRotation > goRotation - 22.5) {
                // Tell the robot to go forward
                return 'f';
            } else {
                if (ourRotation < goRotation) {
                    double diff = goRotation - ourRotation;
                    if (diff <= 180) {
                        // Tell the robot to turn right
                        return 'l';
                    } else {
                        // Tell the robot to turn left
                        return 'r';
                    }
                } else {
                    double diff = ourRotation - goRotation;
                    if (diff <= 180) {
                        return 'r';
                    } else {
                        return 'l';
                    }
                }
            }
        }
    }

    private double getOurRotation(double[] rotation) {

        double x = rotation[0];
        double y = rotation[1];
        double z = rotation[2];
        double w = rotation[3];

        return Math.toDegrees(Math.atan2(2.0*(x*y + w*z), w*w + x*x - y*y - z*z)) + 180;
    }

    public char equalizeRotations(int ourRotation, int goRotation) {
        if (ourRotation < goRotation) {
            double diff = goRotation - ourRotation;

            if (diff < 5) {
                return 's';
            } else if (diff <= 180) {
                // Tell the robot to turn right
                return 'l';
            } else {
                // Tell the robot to turn left
                return 'r';
            }
        } else {
            double diff = ourRotation - goRotation;

            if (diff < 5) {
                return 's';
            } else if (diff <= 180) {
                return 'r';
            } else {
                return 'l';
            }
        }
    }

}