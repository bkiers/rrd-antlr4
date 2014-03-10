package nl.bigo.antlr4doc;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringReader;

public class Main {

    public Main() {

        String dsl = "Diagram(\n" +
                "  Optional('+', 'skip'),\n" +
                "  Choice(0,\n" +
                "    NonTerminal('name-start char'),\n" +
                "    NonTerminal('escape')),\n" +
                "    ZeroOrMore(\n" +
                "      Choice(0,\n" +
                "        NonTerminal('name char'),\n" +
                "        NonTerminal('escape'))))";

        String script = Utils.slurp(getClass().getResourceAsStream("/railroad-diagram.js"));
        String css = Utils.slurp(getClass().getResourceAsStream("/railroad-diagram.css"));

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        try {
            engine.eval(script);
            String svg = (String) engine.eval(dsl + ".toString()");

            svg = svg.replaceFirst("<svg ", "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ");

            svg = svg.replaceFirst("<g ", "<style type=\"text/css\">" + css + "</style>\n<g ");

            System.out.println(svg);

            PNGTranscoder transcoder = new PNGTranscoder();

            TranscoderInput input = new TranscoderInput(new StringReader(svg));
            OutputStream stream = new FileOutputStream("diagram.png");
            TranscoderOutput output = new TranscoderOutput(stream);

            // Save the image.
            transcoder.transcode(input, output);
            stream.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        String fileName = "src/test/resources/IRI.g4";
        String ruleName = "dec_octet";

        DiagramGenerator generator = new DiagramGenerator(fileName);

        System.out.println(generator.getSVG(ruleName));

        if (generator.createDiagram(ruleName)) {
            System.out.println("OK");
        }
        else {
            System.out.println("oops...");
        }


//        ANTLRv4Lexer lexer = new ANTLRv4Lexer(new ANTLRInputStream(new FileInputStream(fileName)));
//        ANTLRv4Parser parser = new ANTLRv4Parser(new CommonTokenStream(lexer));
//
//        ParseTree tree = parser.grammarSpec();
//        RuleVisitor visitor = new RuleVisitor();
//        visitor.visit(tree);
//
//        System.out.println(visitor.getRules());
//        System.out.println(visitor.getDiagram("dec_octet"));
    }
}