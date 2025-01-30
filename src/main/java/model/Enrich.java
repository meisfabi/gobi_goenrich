package model;

import java.util.Objects;

public class Enrich {
    private final String geneId;
    private final double foldChange;
    private final boolean signIf;

    public Enrich(String geneId, double foldChange, boolean signIf){
        this.geneId = geneId;
        this.foldChange = foldChange;
        this.signIf = signIf;
    }

    public String getGeneId() {
        return geneId;
    }

    public double getFoldChange() {
        return foldChange;
    }

    public boolean isSignIf() {
        return signIf;
    }

    @Override
    public int hashCode() {
        return Objects.hash(geneId);
    }
}
