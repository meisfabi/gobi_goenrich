import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
public class GoParser {
    private static final Logger logger = LoggerFactory.getLogger(GoParser.class);
    public static Map<String, Set<String>> parse(String gafFilePath) {
        Map<String, Set<String>> geneToGo = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(Paths.get(gafFilePath))), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("!")) continue;

                String[] parts = line.split("\t");
                if (parts.length < 5) continue;

                String geneId = parts[2];
                String qualifier = parts[3];
                String goId = parts[4];

                if (!qualifier.isEmpty()) {
                    continue;
                }

                geneToGo.computeIfAbsent(geneId, k -> new HashSet<>()).add(goId);
            }

        } catch (IOException e) {
            logger.error("Error while parsing mapping go mapping file", e);
        }

        return geneToGo;
    }

}

