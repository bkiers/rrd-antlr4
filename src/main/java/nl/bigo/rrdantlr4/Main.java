package nl.bigo.rrdantlr4;

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

        if (args.length != 1) {
            System.err.println("usage: java -jar rrd-antlr4-0.1.0.jar GRAMMAR_FILE");
            System.exit(1);
        }

        String fileName = args[0];

        System.out.println("parsing: " + fileName + " ...");

        DiagramGenerator generator = new DiagramGenerator(fileName);

        System.out.println("creating png images from all grammar rules...");

        for (String rule : generator.getRules().keySet()) {
            generator.createDiagram(rule);
        }

        System.out.println("creating an html page of the grammar...");

        generator.createHtml();

        System.out.println("finished");
    }
}