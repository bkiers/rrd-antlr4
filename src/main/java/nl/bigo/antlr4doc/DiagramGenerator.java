package nl.bigo.antlr4doc;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.util.Map;

public class DiagramGenerator {

    private static final ScriptEngineManager MANAGER = new ScriptEngineManager();
    private static final ScriptEngine ENGINE = MANAGER.getEngineByName("JavaScript");
    private static final String SCRIPT = Utils.slurp(DiagramGenerator.class.getResourceAsStream("/railroad-diagram.js"));
    private static final String CSS = Utils.slurp(DiagramGenerator.class.getResourceAsStream("/railroad-diagram.css"));

    static {
        try {
            ENGINE.eval(SCRIPT);
        }
        catch (ScriptException e) {
            e.printStackTrace();
            System.err.println("could not evaluate script: " + SCRIPT);
            System.exit(1);
        }
    }

    private final String antlr4GrammarFileName;
    private final Map<String, String> rules;

    public DiagramGenerator(String antlr4GrammarFileName) throws IOException {
        this.antlr4GrammarFileName = antlr4GrammarFileName;
        this.rules = parse();
    }

    private Map<String, String> parse() throws IOException {

        ANTLRv4Lexer lexer = new ANTLRv4Lexer(new ANTLRInputStream(new FileInputStream(antlr4GrammarFileName)));
        ANTLRv4Parser parser = new ANTLRv4Parser(new CommonTokenStream(lexer));

        ParseTree tree = parser.grammarSpec();
        RuleVisitor visitor = new RuleVisitor();
        visitor.visit(tree);

        return visitor.getRules();
    }

    public String getSVG(String ruleName) {

        try {
            CharSequence dsl = rules.get(ruleName);

            if (dsl == null) {
                throw new RuntimeException("no such rule found: " + ruleName);
            }

            System.out.println(dsl + "\n=====================================");

            String svg = (String) ENGINE.eval(dsl.toString());

            svg = svg.replaceFirst("<svg ", "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ");
            svg = svg.replaceFirst("<g ", "<style type=\"text/css\">" + CSS + "</style>\n<g ");

            System.out.println(svg + "\n=====================================");

            return svg;
        }
        catch (ScriptException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean createDiagram(String ruleName) {

        String svg = getSVG(ruleName);

        if (svg == null) {
            return false;
        }

        OutputStream stream = null;

        try {
            PNGTranscoder transcoder = new PNGTranscoder();

            TranscoderInput input = new TranscoderInput(new StringReader(svg));
            stream = new FileOutputStream("./diagrams/" + ruleName + ".png");
            TranscoderOutput output = new TranscoderOutput(stream);

            // Save the image.
            transcoder.transcode(input, output);
            stream.close();

            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore this.
                }
            }
        }
    }
}
