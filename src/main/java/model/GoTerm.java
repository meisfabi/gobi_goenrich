package model;

public class GoTerm {
    private final String geneId;
    private final int size;

    public GoTerm(String geneId, int size){
        this.geneId = geneId;
        this.size = size;
    }
    public String getGeneId() {
        return geneId;
    }

    public int getSize() {
        return size;
    }
}
