package parser;

import model.Enrich;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnrichParser {
    private static final Logger logger = LoggerFactory.getLogger(EnrichParser.class);
    public static Map<String, Enrich> parse(String enrichFilePath, HashSet<String> significantIds) {

        var enrichEntries = new HashMap<String, Enrich>();
        try (BufferedReader reader = new BufferedReader(new FileReader(enrichFilePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")){
                    significantIds.add(line.substring(1, line.length() - 1));
                    continue;
                }

                var parts = line.split("\t");
                if (parts.length < 3) continue;

                String geneId;
                double fc;
                boolean ifSign;
                try{
                    geneId = parts[0];
                    fc = Double.parseDouble(parts[1]);
                    ifSign = Boolean.parseBoolean(parts[2]);
                } catch (Exception e){
                    continue;
                }

                enrichEntries.computeIfAbsent(geneId, id -> new Enrich(geneId, fc, ifSign));
            }

        } catch (IOException e) {
            logger.error("Error while parsing mapping go mapping file", e);
        }

        return enrichEntries;
    }
}
