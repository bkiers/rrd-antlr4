package nl.bigo.rrdantlr4;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Declares the command line options accepted by the main application.
 */
public class RrdAntlrOptions {
    @Argument(required = true, metaVar = "GRAMMAR_FILE", usage = "An ANTLR4 grammar file to process")
    private String inputFileName;

    @Option(name="--out", metaVar = "HTML_FILE", usage = "The HTML file where the resulting diagrams are generated." +
            "\nDefault is index.html")
    private String outputFileName = "index.html";

    @Option(name="--help", aliases = {"-?","-h"}, help = true, usage = "Show the command line usage and exit")
    private boolean requestingHelp;

    public String getInputFileName() {
        return inputFileName;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public boolean isRequestingHelp() {
        return requestingHelp;
    }
}
