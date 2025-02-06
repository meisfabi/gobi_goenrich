package model;

import java.util.List;

public class State {
    private final String node;
    private final boolean hasMovedUp;
    private List<String> path;
    private String lcaCandidate;
    private Direction direction;
    private int distance;

    public State(String node, boolean hasMovedUp, List<String> path, String lcaCandidate) {
        this.node = node;
        this.hasMovedUp = hasMovedUp;
        this.path = path;
        this.lcaCandidate = lcaCandidate;
    }


    public Direction getDirection() {
        return direction;
    }

    public List<String> getPath() {
        return path;
    }

    public int getDistance() {
        return distance;
    }

    public String getLcaCandidate() {
        return lcaCandidate;
    }

    public String getNode() {
        return node;
    }

    public boolean hasMovedUp() {
        return hasMovedUp;
    }
}