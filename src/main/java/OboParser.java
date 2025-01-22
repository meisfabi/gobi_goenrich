import model.Obo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class OboParser {
    private static final Logger logger = LoggerFactory.getLogger(OboParser.class);
    private final Map<String, Obo> obos = new HashMap<>();
    public void parse(String inputPath){
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

    }

    private static boolean inTerm = false;
    private Obo currentObo;
    private void processLine(String line){
        if(!inTerm && line.isEmpty()){
            return;
        }
        var isLabel = true;
        var label = new StringBuilder();
        var value = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            var currentChar = line.charAt(i);
            if (isLabel && currentChar == ':') {
               isLabel = false;
            } else if(currentChar == '!'){
                break;
            }else if (isLabel) {
                label.append(currentChar);
            } else {
                value.append(currentChar);
            }
        }
        var labelString = label.toString();

        if(labelString.equals("[Term]")){
            inTerm = true;
            currentObo = new Obo();
            return;
        } else if (labelString.isEmpty()) {
            inTerm = false;
            obos.put(currentObo.getId(), currentObo);
            currentObo = null;
            return;
        } else if(!inTerm){
            return;
        }

        switch(labelString){
            case "id":
                currentObo.setId(value.toString().strip());
                break;
            case "name":
                currentObo.setName(value.toString().strip());
                break;
            case "namespace":
                currentObo.setNamespace(value.toString().strip());
                break;
            case "is_a":
                currentObo.getIsA().add(value.toString().strip());
                break;
        }
    }
}
