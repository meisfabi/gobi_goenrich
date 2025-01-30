package parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class EnsemblParser {
    private static final Logger logger = LoggerFactory.getLogger(GoParser.class);
    public static Map<String, Set<String>> parse(String ensemblFilePath) {
        var geneToGo = new HashMap<String, Set<String>>();

        try (BufferedReader reader = new BufferedReader(new FileReader(ensemblFilePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("!")) continue;

                var parts = line.split("\t");
                if (parts.length < 3) continue;

                var geneId = parts[1];
                if (geneId.isEmpty()) {
                    continue;
                }

                var goIds = geneToGo.computeIfAbsent(geneId, k -> new HashSet<>());

                goIds.addAll(Arrays.asList(parts[2].split("\\|")));
            }

        } catch (IOException e) {
            logger.error("Error while parsing mapping go mapping file", e);
        }

        return geneToGo;
    }
}
