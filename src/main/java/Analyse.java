import model.Enrich;
import model.Obo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Analyse {

    private static void computeSize(Map<String, Set<String>> mapping, Map<String, Enrich> enrichSet, Map<String, Obo> obos, int minSize, int maxSize) {
        var gos = new HashMap<String, Integer>();
        for(var entry : obos.entrySet()){
            var goId = entry.getKey();
            var obo = entry.getValue();

            var numGenes = obo.getAssociatedGenes().size();
            if(numGenes < minSize || numGenes > maxSize){
                continue;
            }

            gos.put(goId, numGenes);
        }
        var a = 2;
    }

    public static void compute(Map<String, Obo> obos, Map<String, Set<String>> mapping, Map<String, Enrich> enrichSet, int minSize, int maxSize, String output) {
        computeSize(mapping, enrichSet, obos, minSize, maxSize);
        var a = 2;
    }
}
