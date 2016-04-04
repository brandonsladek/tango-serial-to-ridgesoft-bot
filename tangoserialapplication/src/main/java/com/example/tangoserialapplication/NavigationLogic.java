package com.example.tangoserialapplication;

/**
 * Created by brandonsladek on 3/30/16.
 */

public class NavigationLogic {

    private TangoSerialConnection tsConn;

    // Constructor
    public NavigationLogic(TangoSerialConnection tsConn) {
        this.tsConn = tsConn;
    }


    //    public double GetDistanceToObject() {
//        Grid sphereObject = GameObject.Find ("Sphere").transform.position;
//        Vector3 myPosition = m_tangoPosition;
//
//        double xDiff = Math.Abs(sphereObject.x - myPosition.x);
//        double zDiff = Math.Abs(sphereObject.z - myPosition.z);
//
//        double sumOfSquares = Math.Pow (xDiff, 2) + Math.Pow (zDiff, 2);
//
//        double distance = Math.Sqrt (sumOfSquares);
//
//        distanceText.text = "Distance To Object: " + distance;
//
//        return distance;
//    }
//
//    public double GetDistance(Vector3 location)
//    {
//        Vector3 sphereObject = GameObject.Find ("Sphere").transform.position;
//
//        double xDiff = Math.Abs(sphereObject.x - location.x);
//        double zDiff = Math.Abs(sphereObject.z - location.z);
//
//        double sumOfSquares = Math.Pow (xDiff, 2) + Math.Pow (zDiff, 2);
//
//        double distance = Math.Sqrt (sumOfSquares);
//
//        return distance;
//    }
//
//    public void NavigationAlgorithmOne()
//    {
//        Vector3 myPosition = m_tangoPosition;
//
//        double currentDistance = GetDistanceToObject ();
//
//        float radiusDiff = 2.0f;
//        float xDiff = (float) (radiusDiff * Math.Cos (45.0));
//        float zDiff = (float) (radiusDiff * Math.Sin (45.0));
//
//        Vector3 north = new Vector3 (myPosition.x, myPosition.y, myPosition.z + radiusDiff);
//        Vector3 northEast = new Vector3 (myPosition.x + xDiff, myPosition.y, myPosition.z + zDiff);
//        Vector3 east = new Vector3 (myPosition.x + radiusDiff, myPosition.y, myPosition.z);
//        Vector3 southEast = new Vector3 (myPosition.x + xDiff, myPosition.y, myPosition.z - zDiff);
//        Vector3 south = new Vector3 (myPosition.x, myPosition.y, myPosition.z - radiusDiff);
//        Vector3 southWest = new Vector3 (myPosition.x - xDiff, myPosition.y, myPosition.z - zDiff);
//        Vector3 west = new Vector3 (myPosition.x - radiusDiff, myPosition.y, myPosition.z);
//        Vector3 northWest = new Vector3 (myPosition.x - xDiff, myPosition.y, myPosition.z + zDiff);
//
//        double[] distances = new double[8];
//
//        double distanceNorth = GetDistance (north);
//        double distanceNorthEast = GetDistance (northEast);
//        double distanceEast = GetDistance (east);
//        double distanceSouthEast = GetDistance (southEast);
//        double distanceSouth = GetDistance (south);
//        double distanceSouthWest = GetDistance (southWest);
//        double distanceWest = GetDistance (west);
//        double distanceNorthWest = GetDistance (northWest);
//
//        distances [0] = distanceNorth;
//        distances [1] = distanceNorthEast;
//        distances [2] = distanceEast;
//        distances [3] = distanceSouthEast;
//        distances [4] = distanceSouth;
//        distances [5] = distanceSouthWest;
//        distances [6] = distanceWest;
//        distances [7] = distanceNorthWest;
//
//        int indexWithMinimumDistance = 8;
//        double minimumDistance = double.MaxValue;
//
//        for (int i = 0; i < distances.Length; i++)
//        {
//            if (distances [i] < minimumDistance)
//            {
//                indexWithMinimumDistance = i;
//                minimumDistance = distances [i];
//            }
//        }
//
//        double ourYValue = m_tangoRotation.eulerAngles.y;
//        double getToYValue = ConvertIndexToYRotationValue (indexWithMinimumDistance);
//
//        directionHelperText.text = "Get to y value: " + getToYValue;
//
//        GetClosestDirection (ourYValue, getToYValue);
//    }
//
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

//
//    public void GetClosestDirection(double OurYRotation, double GoYRotation)
//    {
//        if (OurYRotation < GoYRotation + 15 && OurYRotation > GoYRotation - 15) {
//            distanceText.text = "Go Straight!";
//        } else {
//            int stepsRight = 100;
//            int stepsLeft = 100;
//            double adder = 10;
//            double angleL = OurYRotation;
//            double angleR = OurYRotation;
//
//            if (angleL < GoYRotation) {
//                double diff = GoYRotation - angleL;
//                if (diff <= 180) {
//                    distanceText.text = "Turn Right!";
//                } else {
//                    distanceText.text = "Turn Left!";
//                }
//            } else {
//                double diff = angleL - GoYRotation;
//                if (diff <= 180) {
//                    distanceText.text = "Turn Left!";
//                } else {
//                    distanceText.text = "Turn Right!";
//                }
//            }
//        }
//    }
}