import model.Direction;
import model.Enrich;
import model.Obo;
import model.State;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Analyse {

    private final static Logger logger = LoggerFactory.getLogger(Analyse.class);

    public static void compute(Map<String, Obo> obos, Map<String, Set<String>> mapping, Map<String, Enrich> enrichSet, int minSize, int maxSize, String output, String outputOverlap, HashSet<String> groundTruth, Set<String> overallGenes, Set<String> signOverallGenes) {
        for (var goId : groundTruth) {
            var obo = obos.get(goId);
            if (obo == null)
                continue;

            obo.setGroundTruth(true);
        }
        var validObos = computeSize(mapping, enrichSet, obos, minSize, maxSize);
        calculateStats(obos, validObos, overallGenes, signOverallGenes, enrichSet);
        getShortestPath(obos, groundTruth, validObos);
        print(obos, validObos, output, groundTruth);
        if(outputOverlap != null && !outputOverlap.isEmpty())
            calculateOverlap(obos, validObos, outputOverlap);
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

    private record GoResult(String goId, double hgPval, double fejPval, double ksPval) {
    }

    private static void calculateStats(Map<String, Obo> obos, Set<String> overlapping,
                                       Set<String> overallGenes, Set<String> signOverallGenes,
                                       Map<String, Enrich> enrichSet) {
        var N = overallGenes.size();
        var K = signOverallGenes.size();

        var pValsHyper = new TreeMap<Double, List<String>>();
        var pValsFishers = new TreeMap<Double, List<String>>();
        var pValsKol = new TreeMap<Double, List<String>>();
        var m = overlapping.size();

        var overallSize = overallGenes.size();
        var overallGeneArray = overallGenes.toArray(new String[overallSize]);
        var overallFoldChangeArray = new double[overallSize];
        int i = 0;
        for (var gene : overallGenes) {
            overallFoldChangeArray[i++] = enrichSet.get(gene).getFoldChange();
        }

        var ksTest = new KolmogorovSmirnovTest();

        var results = overlapping
                .parallelStream()
                .map(goId -> {
                    var obo = obos.get(goId);

                    int n = obo.getSize();
                    int k = obo.getOverlappingGenes().size();

                    // Hypergeometrisch
                    var hgDist = new HypergeometricDistribution(N, K, n);
                    var hgPval = hgDist.upperCumulativeProbability(k);
                    obo.setHgPval(hgPval);

                    // Fisher
                    var fishers = new HypergeometricDistribution(N - 1, K - 1, n - 1);
                    var fisherPval = fishers.upperCumulativeProbability(k - 1);
                    obo.setFejPval(fisherPval);

                    // KS
                    var associatedGenes = obo.getAssociatedGenes();
                    var foregroundArray = associatedGenes.stream()
                            .filter(enrichSet::containsKey)
                            .mapToDouble(gene -> enrichSet.get(gene).getFoldChange())
                            .toArray();

                    // Background
                    List<Double> bgList = new ArrayList<>(overallSize - associatedGenes.size());
                    for (int j = 0; j < overallSize; j++) {
                        if (!associatedGenes.contains(overallGeneArray[j])) {
                            bgList.add(overallFoldChangeArray[j]);
                        }
                    }
                    var backgroundArray = bgList.stream().mapToDouble(Double::doubleValue).toArray();

                    var ksStat = ksTest.kolmogorovSmirnovStatistic(foregroundArray, backgroundArray);
                    var ksPval = ksTest.kolmogorovSmirnovTest(foregroundArray, backgroundArray);
                    obo.setKsStat(ksStat);
                    obo.setKsPval(ksPval);

                    return new GoResult(goId, hgPval, fisherPval, ksPval);
                })
                .toList();

        for (var r : results) {
            pValsHyper.computeIfAbsent(r.hgPval(), id -> new ArrayList<>()).add(r.goId());
            pValsFishers.computeIfAbsent(r.fejPval(), id -> new ArrayList<>()).add(r.goId());
            pValsKol.computeIfAbsent(r.ksPval(), id -> new ArrayList<>()).add(r.goId());
        }

        // BH-Korrektur
        applyBHCorrection(pValsHyper, obos, m, Obo::getHgFdr, Obo::setHgFdr);
        applyBHCorrection(pValsFishers, obos, m, Obo::getFejFdr, Obo::setFejFdr);
        applyBHCorrection(pValsKol, obos, m, Obo::getKsFdr, Obo::setKsFdr);
    }

    private static void applyBHCorrection(TreeMap<Double, List<String>> pValsMap,
                                          Map<String, Obo> obos,
                                          int m,
                                          Function<Obo, Double> getter,
                                          BiConsumer<Obo, Double> setter) {
        var rank = 1;
        for (var entry : pValsMap.entrySet()) {
            var pVal = entry.getKey();
            var numTerms = entry.getValue().size();
            var effectiveRank = rank + numTerms - 1;
            var bhPval = Math.min(1.0, (pVal * m) / effectiveRank);

            for (var goId : entry.getValue()) {
                var obo = obos.get(goId);
                setter.accept(obo, bhPval);
            }
            rank += numTerms;
        }

        var minBH = 1.0;
        for (var entry : pValsMap.descendingMap().entrySet()) {
            for (var goId : entry.getValue()) {
                var obo = obos.get(goId);
                var currentBH = getter.apply(obo);
                minBH = Math.min(minBH, currentBH);
                setter.accept(obo, minBH);
            }
        }
    }


    private static void getShortestPath(Map<String, Obo> obos, Set<String> groundTruth, Set<String> validObos) {
        validObos.parallelStream().forEach(goId -> {//for (var goId : validObos) {
            if (!groundTruth.contains(goId)) {
                var result = findShortestPath(goId, obos, groundTruth);
                if (result == null)
                    return;
                var path = result.path;
                var lca = result.lca;
                if (path != null && !path.isEmpty()) {
                    var pathString = String.join("|", path.stream()
                            .map(id -> {
                                var name = obos.get(id).getName();
                                if (id.equals(lca)) {
                                    return name + " * ";
                                }
                                return name;
                            })
                            .toList());
                    var obo = obos.get(goId);
                    obo.setShortestPath(pathString);
                }
            }
        });
    }

    record shortestPathResult(List<String> path, String lca) {
    }

    private static shortestPathResult findShortestPath(String startId,
                                                       Map<String, Obo> obos,
                                                       Set<String> groundTruth) {

        var queue = new LinkedList<State>();
        var visited = new HashSet<String>();

        var startKey = startId + "|false";
        visited.add(startKey);
        queue.add(new State(startId, false, new ArrayList<>(List.of(startId)), null));
        while (!queue.isEmpty()) {
            var current = queue.poll();

            if (groundTruth.contains(current.getNode())) {
                return new shortestPathResult(current.getPath(), current.getLcaCandidate());
            }
            var obo = obos.get(current.getNode());
            if (!current.hasMovedUp()) {
                for (var parentId : obo.getIsA()) {
                    var key = parentId + "|false";
                    if (!visited.contains(key)) {
                        visited.add(key);
                        var newPath = new ArrayList<>(current.getPath());
                        newPath.add(parentId);
                        queue.add(new State(parentId, false, newPath, parentId));
                    }
                }
                for (var childId : obo.getChildren()) {
                    var key = childId + "|true";
                    if (!visited.contains(key)) {
                        visited.add(key);
                        var newPath = new ArrayList<>(current.getPath());
                        newPath.add(childId);
                        var newLca = (current.getLcaCandidate() == null) ? current.getNode() : current.getLcaCandidate();
                        queue.add(new State(childId, true, newPath, newLca));
                    }
                }
            } else {
                for (var childrenId : obo.getChildren()) {
                    var key = childrenId + "|true";
                    if (!visited.contains(key)) {
                        visited.add(key);
                        var newPath = new ArrayList<>(current.getPath());
                        newPath.add(childrenId);
                        queue.add(new State(childrenId, true, newPath, current.getLcaCandidate()));
                    }
                }
            }
        }
        return null;
    }

    private static void calculateOverlap(Map<String, Obo> obos, Set<String> validObos, String outputPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("term1\tterm2\tis_relative\tpath_length\tnum_overlapping\tmax_ov_percent\n");
            var validList = new ArrayList<>(validObos);
            var allDistances = new ConcurrentHashMap<String, Map<String, Integer>>();
            validObos.parallelStream().forEach(termId -> {
                var dist = computeDistancesWithSingleSwitch(termId, obos);
                allDistances.put(termId, dist);
            });
            for (int i = 0; i < validList.size(); i++) {
                var termId1 = validList.get(i);
                var term1 = obos.get(termId1);
                for (int j = i + 1; j < validList.size(); j++) {
                    var termId2 = validList.get(j);
                    var term2 = obos.get(termId2);

                    var genes1 = term1.getAssociatedGenes();
                    var genes2 = term2.getAssociatedGenes();

                    int numOverlapping = 0;
                    var smallerGeneSet = genes1.size() <= genes2.size() ? genes1 : genes2;
                    var biggerGeneSet = genes1.size() > genes2.size() ? genes1 : genes2;
                    for (var gene : smallerGeneSet) {
                        if (biggerGeneSet.contains(gene)) {
                            numOverlapping++;
                        }
                    }

                    if (numOverlapping == 0)
                        continue;

                    var divisor = Math.min(genes1.size(), genes2.size());
                    var maxOvPercent = (divisor == 0 ? 0 : (numOverlapping / (double) divisor) * 100);

                    var isRelative = term1.getAllAncestors().contains(termId2) || term2.getAllAncestors().contains(termId1);
                    var shortestPath = allDistances.get(termId1).getOrDefault(termId2, -1);
                    writer.write(termId1 + "\t" + termId2 + "\t" + isRelative + "\t" + shortestPath + "\t" + numOverlapping + "\t" + maxOvPercent + "\n");

                }
            }
        } catch (IOException e) {
            logger.error("Fehler beim Schreiben der overlap_out_tsv Datei", e);
        }

    }

    private static Map<String, Integer> computeDistancesWithSingleSwitch(
            String startId,
            Map<String, Obo> obos
    ) {
        record State(String node, boolean hasMovedUp) {}

        var dist = new HashMap<State, Integer>();

        var queue = new ArrayDeque<State>();
        var visited = new HashSet<State>();

        var start = new State(startId, false);
        dist.put(start, 0);
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            var current = queue.poll();
            var currentObo = obos.get(current.node());
            if (currentObo == null) {
                continue;
            }
            int currentDistance = dist.get(current);

            if (!current.hasMovedUp()) {
                for (String parent : currentObo.getIsA()) {
                    var next = new State(parent, false);
                    if (visited.add(next)) {
                        dist.put(next, currentDistance + 1);
                        queue.add(next);
                    }
                }

                for (String child : currentObo.getChildren()) {
                    var next = new State(child, true);
                    if (visited.add(next)) {
                        dist.put(next, currentDistance + 1);
                        queue.add(next);
                    }
                }

            } else {
                for (String child : currentObo.getChildren()) {
                    var next = new State(child, true);
                    if (visited.add(next)) {
                        dist.put(next, currentDistance + 1);
                        queue.add(next);
                    }
                }
            }
        }

        var finalDistances = new HashMap<String, Integer>();
        for (var entry : dist.entrySet()) {
            var node = entry.getKey().node();
            var distance = entry.getValue();
            finalDistances.merge(node, distance, Math::min);
        }

        return finalDistances;
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
                var hgPval = String.format(Locale.US, "%.5e", obo.getHgPval());
                var hgFdr = String.format(Locale.US, "%.5e", obo.getHgFdr());
                var fishersPval = String.format(Locale.US, "%.5e", obo.getFejPval());
                var fishersFDR = String.format(Locale.US, "%.5e", obo.getFejFdr());
                var ksStat = String.format(Locale.US, "%.5e", obo.getKsStat());
                var ksPval = String.format(Locale.US, "%.5e", obo.getKsPval());
                var ksFdr = String.format(Locale.US, "%.5e", obo.getKsFdr());
                var shortestPath = obo.getShortestPath();
                writer.write(goId + "\t" + name + "\t" + size + "\t" + isTrue + "\t" + nOverlap + "\t" + hgPval + "\t" + hgFdr + "\t" + fishersPval + "\t" + fishersFDR + "\t" + ksStat + "\t" + ksPval + "\t" + ksFdr + "\t" + shortestPath + "\n");
            }
        } catch (Exception e) {
            logger.error("Error while writing to file", e);
        }
    }


}
