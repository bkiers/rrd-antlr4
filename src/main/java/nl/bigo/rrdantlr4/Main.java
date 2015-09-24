package nl.bigo.rrdantlr4;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.PrintStream;

/**
 * A class containing a main method used in the packaged JAR file to
 * parse a grammar provided from the command line.
 */
public class Main {

    /**
     * The entry point for the command line.
     *
     * @param args
     *         the command line parameters, expected is one parameter.
     *
     * @throws Exception
     *         when the grammar is invalid, inaccessible, or if I goofed
     *         things up.
     */
    public static void main(String[] args) throws Exception {

        RrdAntlrOptions rrdAntlrOptions = new RrdAntlrOptions();
        final CmdLineParser cmdLineParser = new CmdLineParser(rrdAntlrOptions);

        try {
            cmdLineParser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            printUsage(cmdLineParser, System.err);
            System.exit(1);
        }

        if (rrdAntlrOptions.isRequestingHelp()) {
            printUsage(cmdLineParser, System.out);
            System.exit(0);
        }

        String fileName = rrdAntlrOptions.getInputFileName();

        System.out.println("parsing: " + fileName + " ...");

        DiagramGenerator generator = new DiagramGenerator(fileName);

        System.out.println("creating png images from all grammar rules...");

        for (String rule : generator.getRules().keySet()) {
            generator.createDiagram(rule);
        }

        System.out.println("creating pdf pages for the grammar...");
        boolean pdfCreated = generator.createPdf(generator.getRules());
        if (pdfCreated) {
            System.out.println("created pdf pages of the grammar...");
        } else {
            System.out.println("creating pdf failed...");
        }


        System.out.println("creating an html page of the grammar...");

        generator.createHtml(rrdAntlrOptions.getOutputFileName());

        System.out.println("finished");
    }

    private static void printUsage(CmdLineParser cmdLineParser, PrintStream out) {
        out.println("usage: java -jar rrd-antlr4-0.1.0.jar [options] GRAMMAR_FILE");
        cmdLineParser.printUsage(out);
    }
}
