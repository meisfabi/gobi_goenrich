package model;

public class Result {
    private final String goId;
    private final String desc;
    private final int size;

    public Result(String goId, String desc, int size) {
        this.goId = goId;
        this.desc = desc;
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public String getDesc() {
        return desc;
    }

    public String getGoId() {
        return goId;
    }
}
