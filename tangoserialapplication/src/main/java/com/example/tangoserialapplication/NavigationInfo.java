package com.example.tangoserialapplication;

/**
 * Created by brandonsladek on 4/23/16.
 */

public class NavigationInfo {

    private int ourRotation;
    private int goRotation;
    private char command;

    public NavigationInfo(int ourRotation, int goRotation, char command) {
        this.ourRotation = ourRotation;
        this.goRotation = goRotation;
        this.command = command;
    }

    public int getOurRotation() {
        return ourRotation;
    }

    public int getGoRotation() {
        return goRotation;
    }

    public char getCommand() {
        return command;
    }
}
