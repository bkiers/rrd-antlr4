package nl.bigo.rrdantlr4;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiagramGenerator {

    private static final ScriptEngineManager MANAGER = new ScriptEngineManager();
    private static final ScriptEngine ENGINE = MANAGER.getEngineByName("JavaScript");
    private static final String RAILROAD_SCRIPT = Utils.slurp(DiagramGenerator.class.getResourceAsStream("/railroad-diagram.js"));
    private static final String RAILROAD_CSS = Utils.slurp(DiagramGenerator.class.getResourceAsStream("/railroad-diagram.css"));
    private static final String HTML_TEMPLATE = Utils.slurp(DiagramGenerator.class.getResourceAsStream("/template.html"));
    private static final String CSS_TEMPLATE = Utils.slurp(DiagramGenerator.class.getResourceAsStream("/template.css"));

    static {
        try {
            ENGINE.eval(RAILROAD_SCRIPT);
        }
        catch (ScriptException e) {
            e.printStackTrace();
            System.err.println("could not evaluate script:\n" + RAILROAD_SCRIPT);
            System.exit(1);
        }
    }

    private final String antlr4Grammar;
    private String antlr4GrammarFileName;
    private String antlr4GrammarName;
    private File outputDir;
    private final Map<String, String> rules;

    public DiagramGenerator(String antlr4Grammar) throws IOException {
        this.antlr4Grammar = antlr4Grammar.trim();
        this.antlr4GrammarFileName = null;
        this.antlr4GrammarName = null;
        this.outputDir = null;
        this.rules = parse();
    }

    private Map<String, String> parse() throws IOException {

        InputStream input;

        File file = new File(antlr4Grammar);

        if (file.exists()) {
            input = new FileInputStream(antlr4Grammar);
            this.antlr4GrammarFileName = file.getName();
        }
        else if (antlr4Grammar.startsWith("http://") || antlr4Grammar.startsWith("https://")) {
            URLConnection connection = new URL(antlr4Grammar).openConnection();
            this.antlr4GrammarFileName = antlr4Grammar.substring(antlr4Grammar.lastIndexOf('/') + 1);
            input = connection.getInputStream();
        }
        else {
            // We'll assume the the string _is_ the ANTLR 4 grammar...
            this.antlr4GrammarFileName = "grammar-" + System.currentTimeMillis();
            input = new ByteArrayInputStream(antlr4Grammar.getBytes("UTF-8"));
        }

        this.antlr4GrammarName = this.antlr4GrammarFileName.replaceAll(".[gG]4$", "");
        this.outputDir = new File("./output", this.antlr4GrammarName);

        if(!this.outputDir.exists() && !this.outputDir.mkdirs()) {
            throw new RuntimeException("could not create output dir: " + this.outputDir);
        }

        ANTLRv4Lexer lexer = new ANTLRv4Lexer(new ANTLRInputStream(new BufferedInputStream(input)));
        ANTLRv4Parser parser = new ANTLRv4Parser(new CommonTokenStream(lexer));

        ParseTree tree = parser.grammarSpec();
        RuleVisitor visitor = new RuleVisitor();
        visitor.visit(tree);

        return visitor.getRules();
    }

    public Map<String, String> getRules() {
        return new LinkedHashMap<String, String>(rules);
    }

    public String getSVG(String ruleName) {

        try {
            CharSequence dsl = rules.get(ruleName);

            if (dsl == null) {
                throw new RuntimeException("no such rule found: " + ruleName);
            }

            String svg = (String) ENGINE.eval(dsl.toString());

            svg = svg.replaceFirst("<svg ", "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ");
            svg = svg.replaceFirst("<g ", "<style type=\"text/css\">" + RAILROAD_CSS + "</style>\n<g ");

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
            stream = new FileOutputStream(new File(this.outputDir, ruleName + ".png"));
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
                }
                catch (IOException e) {
                    // Ignore this.
                }
            }
        }
    }

    public boolean createHtml() {
        return createHtml("index.html");
    }

    public boolean createHtml(String fileName) {

        String template = HTML_TEMPLATE;

        template = template.replace("${grammar}", this.antlr4GrammarFileName);

        template = template.replace("${css}", CSS_TEMPLATE);

        StringBuilder rows = new StringBuilder();

        for (String ruleName : this.rules.keySet()) {
            String svg = this.getSVG(ruleName);
            rows.append("<tr><td id=\"").append(ruleName).append("\">")
                    .append(ruleName).append("</td><td>").append(svg).append("</td></tr>");
        }

        template = template.replace("${rows}", rows);

        template = this.addLinks(fileName, template);

        PrintWriter out = null;

        try {
            out = new PrintWriter(new File(this.outputDir, fileName));
            out.write(template);
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }

    // The pattern matching a SVG text tag, or any other single character.
    private static final Pattern TEXT_PATTERN = Pattern.compile("(<text\\s+[^>]*?>\\s*(.+?)\\s*</text>)|[\\s\\S]");

    /**
     * Returns an HTML template containing SVG text-tags that
     * will be wrapped with '<a xlink:href=...' to make the grammar
     * rules clickable inside the HTML page.
     *
     * @param fileName
     *         the name og the parsed grammar.
     * @param template
     *         the template whose text-tags need to be linked.
     *
     * @return an HTML template containing SVG text-tags that
     * will be wrapped with '<a xlink:href=...' to make the grammar
     * rules clickable inside the HTML page.
     */
    private String addLinks(String fileName, String template) {

        StringBuilder builder = new StringBuilder();
        Matcher m = TEXT_PATTERN.matcher(template);

        while (m.find()) {

            if (m.group(1) == null) {
                // We didn't match a text-tag, just append whatever we did match.
                builder.append(m.group());
            }
            else {
                // We found an SVG text tag.
                String textTag = m.group(1);
                String rule = m.group(2);

                // The rule does not match any of the parser rules (one of:
                // epsilon/not/comment/literal tags probably). Do not link
                // but just add it back in the builder.
                if (!this.rules.containsKey(rule)) {
                    builder.append(textTag);
                }
                else {
                    // Yes, the rule matches with a parsed rule, add a link
                    // around it.
                    builder.append("<a xlink:href=\"").append(fileName)
                            .append("#").append(rule).append("\">")
                            .append(textTag).append("</a>");
                }
            }
        }

        return builder.toString();
    }
}
