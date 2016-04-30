package com.example.tangoserialapplication;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by brandonsladek on 4/29/16.
 */

public class CourseInfo implements Serializable {

    private SafePath safePath;
    private HashMap<String, TargetLocation> targetLocationsByName;

    public CourseInfo(SafePath safePath, HashMap<String, TargetLocation> targetLocationsByName) {
        this.safePath = safePath;
        this.targetLocationsByName = targetLocationsByName;
    }

    public SafePath getSafePath() {
        return safePath;
    }

    public HashMap<String, TargetLocation> getTargetLocationsByName() {
        return targetLocationsByName;
    }
}
