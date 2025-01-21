import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            var start = System.currentTimeMillis();
            var wholeStart = start;
            logger.info(String.format("Time needed for parsing: %s seconds", (System.currentTimeMillis() - start) / 1000.0));
            logger.info(String.format("Time needed for whole program: %s seconds", (System.currentTimeMillis() - wholeStart) / 1000.0));
        } catch(ArgumentParserException e){
            parser.printHelp();
        } catch (Exception e) {
            logger.error("Error while executing main", e);
        }
    }
}