package com.example.tangoserialapplication;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by brandonsladek on 4/25/16.
 */

public class SafePath implements Serializable {

    private ArrayList<SafePoint> safePoints;

    public SafePath(ArrayList<SafePoint> safePoints) {
        this.safePoints = safePoints;
    }

    public ArrayList<SafePoint> getSafePath() {
        return safePoints;
    }

    public void setSafePath(ArrayList<SafePoint> safePoints) {
        this.safePoints = safePoints;
    }

    public String getSafePathString() {
        String points = "";

        for (int i = 0; i < safePoints.size(); i++) {
            points = points + "Point " + i + ": " + safePoints.get(i).getPointAsString() + "\n";
        }
        return points;
    }

    public double[] getClosestSafePathPoint(double[] robotLocation) {

        NavigationLogic navigationLogic = new NavigationLogic();

        double minimumDistance = Double.MAX_VALUE;
        int indexWithMinimumDistance = 0;

        for (int i = 0; i < safePoints.size(); i++) {
            double distance = navigationLogic.getDistance(robotLocation, safePoints.get(i).getPoint());

            if (distance < minimumDistance) {
                minimumDistance = distance;
                indexWithMinimumDistance = i;
            }
        }
        return safePoints.get(indexWithMinimumDistance).getPoint();
    }

}
