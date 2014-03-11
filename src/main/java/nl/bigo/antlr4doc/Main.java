package nl.bigo.antlr4doc;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.err.println("usage: java -jar rrd-antlr4.jar GRAMMAR_FILE");
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

        System.out.println("done!");
    }
}