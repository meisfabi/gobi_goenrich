import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Main").build().defaultHelp(true).description("Program that calculates the PSI values for the exons of a BAM file.");
        parser.addArgument("-gtf").required(true).help("GTF File Path").metavar("<GTF file>").type(String.class);
        parser.addArgument("-bam").required(true).help("BAM File Path").metavar("<BAM file>").type(String.class);
        parser.addArgument("-o").required(true).help("Output Path").metavar("<output file path>").type(String.class);
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