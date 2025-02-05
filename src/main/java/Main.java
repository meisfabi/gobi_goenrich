import model.Enrich;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Main").build().defaultHelp(true).description("Go Enrich");
        parser.addArgument("-obo").required(true).help("OBO File Path").metavar("<OBO file>").type(String.class);
        parser.addArgument("-root").required(true).help("GO namespace").metavar("<GO namespace>").type(String.class);
        parser.addArgument("-mapping").required(true).help("gene2go mapping file").metavar("<gene2go_mapping_file>").type(String.class);
        parser.addArgument("-mappingtype").required(true).help("Mapping Type").metavar("[ensembl|go]").type(String.class);
        parser.addArgument("-enrich").required(true).help("diffexp file").metavar("<diffexp_file>").type(String.class);
        parser.addArgument( "-o").required(true).help("Output Path").metavar("<output file path>").type(String.class);
        parser.addArgument( "-minsize").required(true).help("minsize").metavar("<min size>").type(Integer.class);
        parser.addArgument( "-maxsize").required(true).help("maxsize").metavar("<max size>").type(Integer.class);
        parser.addArgument( "-overlapout").required(false).help("maxsize").metavar("overlap_out_tsv").type(String.class);
        try {
            Namespace res = parser.parseArgs(args);
            var oboParser = new OboParser();
            int minSize = res.get("minsize");
            int maxSize = res.get("maxsize");
            String outputPath = res.get("o");
            String outputOverlapPath = res.get("overlapout");
            var start = System.currentTimeMillis();
            var wholeStart = start;
            Map<String, Set<String>> mapping;
            logger.info("Starting to parse mapping File");
            if(res.get("mappingtype").equals("ensembl")){
                mapping = EnsemblParser.parse(res.get("mapping"));
            } else{
                mapping = GoParser.parse(res.get("mapping"));
            }

            logger.info(String.format("Time needed for Obo parsing: %s seconds", (System.currentTimeMillis() - start) / 1000.0));
            start = System.currentTimeMillis();
            var groundTruth = new HashSet<String>();
            var enrich = EnrichParser.parse(res.get("enrich"), groundTruth);
            logger.info(String.format("Time needed for Enrich parsing: %s seconds", (System.currentTimeMillis() - start) / 1000.0));
            logger.info("Starting to parse Obo File");
            start = System.currentTimeMillis();
            var signOverAllGenes = new HashSet<String>();
            var overAllGenes = new HashSet<String>();
            var obos = oboParser.parse(res.get("obo"), res.get("root"), mapping, enrich, overAllGenes, signOverAllGenes);
            logger.info(String.format("Time needed for Obo parsing: %s seconds", (System.currentTimeMillis() - start) / 1000.0));
            //var yur = TestParser.parse("C:\\Users\\mathi\\Desktop\\gobi_projects\\goenrich\\src\\main\\input\\simul_exp_go_bp_ensembl_min50_max500.enrich.out");
            Analyse.compute(obos, mapping, enrich, minSize, maxSize, outputPath, outputOverlapPath, groundTruth, overAllGenes, signOverAllGenes);
            logger.info(String.format("Time needed for whole program: %s seconds", (System.currentTimeMillis() - wholeStart) / 1000.0));
        } catch(ArgumentParserException e){
            parser.printHelp();
        }
        catch (Exception e) {
            logger.error("Error while executing main", e);
        }
    }
}