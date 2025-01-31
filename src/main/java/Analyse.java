import model.Enrich;
import model.Obo;
import model.Result;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.stat.inference.*;
import org.apache.commons.statistics.inference.FisherExactTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.*;

public class Analyse {

    private final static Logger logger = LoggerFactory.getLogger(Analyse.class);

    public static void compute(Map<String, Obo> obos, Map<String, Set<String>> mapping, Map<String, Enrich> enrichSet, int minSize, int maxSize, String output, HashSet<String> groundTruth, Set<String> overallGenes, Set<String> signOverallGenes) {
        for (var goId : groundTruth) {
            var obo = obos.get(goId);
            if (obo == null)
                continue;

            obo.setGroundTruth(true);
        }
        var gurt = computeSize(mapping, enrichSet, obos, minSize, maxSize);
        hyperGeom(obos, gurt, overallGenes, signOverallGenes);
        print(obos, gurt, output, groundTruth);
    }

    private static Set<String> computeSize(Map<String, Set<String>> mapping, Map<String, Enrich> enrichSet, Map<String, Obo> obos, int minSize, int maxSize) {
        var overlapping = new HashSet<String>();
        for (var entry : obos.entrySet()) {
            var goId = entry.getKey();
            var obo = entry.getValue();

            var associatedGenes = obo.getAssociatedGenes();
            var numAssociatedGenes = associatedGenes.size();
            var numGenes = obo.getNotEnrichedGenes().size() + numAssociatedGenes;
            if (numGenes < minSize || numGenes > maxSize) {
                continue;
            }

            obo.setSize(numAssociatedGenes);

            overlapping.add(goId);
            var overlappingGenes = obo.getOverlappingGenes();
            for (var gene : associatedGenes) {
                if (enrichSet.get(gene).isSignIf())
                    overlappingGenes.add(gene);
            }
        }

        return overlapping;
    }

    private static void hyperGeom(Map<String, Obo> obos, Set<String> overlapping, Set<String> overallGenes, Set<String> signOverallGenes){
        var N = overallGenes.size();
        var K = signOverallGenes.size();

        var pValsHyper = new TreeMap<Double, List<String>>();
        var pValsFishers = new TreeMap<Double, List<String>>();
        int m = overlapping.size();
        for(var goId : overlapping){
            var obo = obos.get(goId);

            var n = obo.getSize();// + obo.getNotEnrichedGenes().size();
            var k = obo.getOverlappingGenes().size();

            var hgDist = new HypergeometricDistribution(N, K, n);
            var hgPval = hgDist.upperCumulativeProbability(k);

            obo.setHgPval(hgPval);
            pValsHyper.computeIfAbsent(hgPval, id -> new ArrayList<>()).add(goId);

            var fishers = new HypergeometricDistribution(N - 1, K - 1, n - 1);
            var fishersPval = fishers.upperCumulativeProbability(k - 1);
            obo.setFejPval(fishersPval);

            pValsFishers.computeIfAbsent(fishersPval, id -> new ArrayList<>()).add(goId);
        }
        // BH
        var rank = 1;
        for (var entry : pValsHyper.entrySet()) {
            var pVal = entry.getKey();
            var numTerms = entry.getValue().size();
            var bhPval = Math.min(1.0, ((pVal * m) / rank));

            for (var goId : entry.getValue()) {
                var obo = obos.get(goId);
                obo.setHgFdr(bhPval);
            }

            rank += numTerms;
        }

        rank = 1;

        for (var entry : pValsFishers.entrySet()) {
            var pVal = entry.getKey();
            var numTerms = entry.getValue().size();
            var bhPval = Math.min(1.0, ((pVal * m) / rank));

            for (var goId : entry.getValue()) {
                var obo = obos.get(goId);
                obo.setFejFdr(bhPval);
            }

            rank += numTerms;
        }
    }


    private static void print(Map<String, Obo> obos, Set<String> overlapping, String outputPath, Set<String> groundTruth) {
        try (var writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("term\tname\tsize\tis_true\tnoverlap\thg_pval\thg_fdr\tfej_pval\tfej_fdr\tks_stat\tks_pval\tks_fdr\tshortest_path_to_a_true\n");
            for (var goId : overlapping) {
                var obo = obos.get(goId);
                var name = obo.getName();
                var size = obo.getSize();
                var isTrue = obo.isGroundTruth();
                var nOverlap = obo.getOverlappingGenes().size();
                var hgPval = String.format(Locale.US,"%.5e", obo.getHgPval());
                var hgFdr = String.format(Locale.US,"%.5e", obo.getHgFdr());
                var fishersPval = String.format(Locale.US,"%.5e", obo.getFejPval());
                var fishersFDR = String.format(Locale.US,"%.5e", obo.getFejFdr());
                writer.write(goId + "\t" + name + "\t" + size + "\t" + isTrue + "\t" + nOverlap + "\t" + hgPval + "\t" + hgFdr + "\t" + fishersPval + "\t" + fishersFDR + "\t0\t0\t0\t " + "\n");
            }
        } catch (Exception e) {
            logger.error("Error while writing to file", e);
        }

    }
}
