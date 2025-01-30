package parser;

import model.Result;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TestParser {
    public static Map<String, Result> parse(String ensemblFilePath) {
        var geneToGo = new HashMap<String, Result>();

        try (BufferedReader reader = new BufferedReader(new FileReader(ensemblFilePath))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("GO")) continue;

                var parts = line.split("\t");

                var goId = parts[0];
                var desc = parts[1];
                var size = parts[2];

                geneToGo.computeIfAbsent(goId, k -> new Result(goId, desc, Integer.parseInt(size)));

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return geneToGo;
    }
}
