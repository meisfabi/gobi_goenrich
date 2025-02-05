package model;

public class KsResult {
    private double stat;
    private double pval;
    public KsResult(double stat, double pval) {
        this.stat = stat;
        this.pval = pval;
    }

    public double getPval() {
        return pval;
    }

    public double getStat() {
        return stat;
    }
}
