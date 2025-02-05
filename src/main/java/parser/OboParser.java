package parser;

import model.Enrich;
import model.Obo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class OboParser {
    private static final Logger logger = LoggerFactory.getLogger(OboParser.class);
    public static String root;
    public static Map<String, Set<String>> mapping;
    private static final Map<String, Obo> obos = new HashMap<>();
    private static Map<String, Enrich> enrichMap = new HashMap<>();
    private static Set<String> overallGenes = new HashSet<>();
    private static Set<String> signOverallGenes = new HashSet<>();

    public Map<String, Obo> parse(String inputPath, String r, Map<String, Set<String>> m, Map<String, Enrich> en, Set<String> oaGenes, Set<String> sOaGenes) {
        root = r;
        mapping = m;
        enrichMap = en;
        overallGenes = oaGenes;
        signOverallGenes = sOaGenes;
        Go2GenesMapping();
        int errorLines = 0;
        logger.info("Starting to parse gtf file");
        Path path = Path.of(inputPath);

        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines//.parallel()
                    .filter(line -> !line.startsWith("#"))
                    .forEach(line -> processLine(line.trim()));
        } catch (Exception e) {
            logger.error("Error while parsing gtf file", e);
        }

        logger.info("GTF-File parsed");
        if (errorLines > 0)
            logger.warn(String.format("%s could not be saved due to an error while parsing", errorLines));

        addChildren();
        topologicalDfsSort();
        addGenes();

        return obos;
    }

    private static boolean inTerm = false;
    private Obo currentObo;

    private void processLine(String line) {
        if (!inTerm && line.isEmpty()) {
            return;
        }
        var isLabel = true;
        var label = new StringBuilder();
        var value = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            var currentChar = line.charAt(i);
            if (isLabel && currentChar == ':') {
                isLabel = false;
            } else if (currentChar == '!') {
                break;
            } else if (isLabel) {
                label.append(currentChar);
            } else {
                value.append(currentChar);
            }
        }
        var labelString = label.toString();

        if (labelString.equals("[Term]")) {
            inTerm = true;
            currentObo = new Obo();
            return;
        } else if (labelString.isEmpty()) {
            inTerm = false;
            var genes = Go2Genes.get(currentObo.getId());

            if(genes != null){
                for(var gene : genes){
                    var enrich = enrichMap.get(gene);
                    if(enrich != null){
                        currentObo.getAssociatedGenes().add(gene);
                        overallGenes.add(gene);
                        if(enrich.isSignIf())
                            signOverallGenes.add(gene);
                    } else{
                        currentObo.getNotEnrichedGenes().add(gene);
                    }
                }


            }
            obos.put(currentObo.getId(), currentObo);
            currentObo = null;
            return;
        } else if (!inTerm) {
            return;
        }

        switch (labelString) {
            case "id":
                currentObo.setId(value.toString().strip());
                break;
            case "name":
                currentObo.setName(value.toString().strip());
                break;
            case "namespace":
                var val = value.toString().strip();
                if (!val.equals(root)) {
                    currentObo = null;
                    inTerm = false;
                    return;
                }
                currentObo.setNamespace(value.toString().strip());
                break;
            case "is_a":
                currentObo.getIsA().add(value.toString().strip());
                break;
            case "is_obsolete":
                if (value.toString().strip().equals("true")) {
                    inTerm = false;
                    currentObo = null;
                    return;
                }
        }
    }

    private void addChildren() {
        for (var oboEntry : obos.entrySet()) {
            var child = oboEntry.getValue();
            var childId = oboEntry.getKey();
            for (var parent : child.getIsA()) {
                var parentObo = obos.getOrDefault(parent, null);

                if (parentObo == null) {
                    continue;
                }

                parentObo.getChildren().add(childId);
            }
        }
    }

    private static final Map<String, Set<String>> Go2Genes = new HashMap<>();
    private static void Go2GenesMapping(){
        for(var geneEntry : mapping.entrySet()){
            var geneId = geneEntry.getKey();

            for(var goId : geneEntry.getValue()){
                var set = Go2Genes.computeIfAbsent(goId, id -> new HashSet<>());
                set.add(geneId);
            }
        }
    }


    static List<Obo> sorted = new ArrayList<>();
    static Set<String> visited = new HashSet<>();

    private static void dfs(Obo obo){
        if(obo == null)
            return;
        var goId = obo.getId();

        visited.add(goId);
        for(var parentId : obo.getIsA()){

            if(!visited.contains(parentId)){
                dfs(obos.get(parentId));
            }
        }

        sorted.add(obo);
    }

    private static void topologicalDfsSort(){
        for(var obo : obos.entrySet()){
            var goId = obo.getKey();
            if(!visited.contains(goId)){
                if(obo.getValue() != null){
                    dfs(obo.getValue());
                }
            }
        }

        Collections.reverse(sorted);
    }

    private static void addGenes(){
        for (var child : sorted) {
            var childGenes = child.getAssociatedGenes();
            var childNotOverlappingGenes = child.getNotEnrichedGenes();
            for (var parentId : child.getIsA()) {
                var parent = obos.get(parentId);
                if (parent != null) {
                    parent.getAssociatedGenes().addAll(childGenes);
                    parent.getNotEnrichedGenes().addAll(childNotOverlappingGenes);
                }
            }
        }
    }

}
