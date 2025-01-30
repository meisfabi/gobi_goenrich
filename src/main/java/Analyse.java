import model.Enrich;
import model.Obo;
import model.Result;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Analyse {

    private static HashMap<String, Integer> computeSize(Map<String, Set<String>> mapping, Map<String, Enrich> enrichSet, Map<String, Obo> obos, int minSize, int maxSize, Map<String, Result> yur) {
        var overlapping = new HashMap<String, Integer>();
        for(var entry : obos.entrySet()){
            var goId = entry.getKey();
            var obo = entry.getValue();

            var numGenes = obo.getNotEnrichedGenes().size() + obo.getAssociatedGenes().size();
            if(numGenes < minSize || numGenes > maxSize){
                continue;
            }

            overlapping.put(goId, numGenes);
        }

        return overlapping;
    }

    public static void compute(Map<String, Obo> obos, Map<String, Set<String>> mapping, Map<String, Enrich> enrichSet, int minSize, int maxSize, String output, HashSet<String> groundTruth, Map<String, Result> yur) {
        for(var goId : groundTruth){
            obos.get(goId).setGroundTruth(true);
        }
        var gurt = computeSize(mapping, enrichSet, obos, minSize, maxSize, yur);

    }
}
