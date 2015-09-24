package nl.bigo.rrdantlr4;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.codec.PngImage;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The workhorse of this little library: it parses the ANTLR 4
 * grammar, and invokes the JavaScript library that translates
 * the DSL produces by the ANTLR parser to a SVG.
 *
 * Through this class you can also create an html page containing
 * railroad diagrams of all parsed rules and create individual png
 * images from one (or more) grammar rules.
 */
public class DiagramGenerator {

    // Java's built-in JS interpreter (Rhino).
    private static final ScriptEngineManager MANAGER = new ScriptEngineManager();
    private static final ScriptEngine ENGINE = MANAGER.getEngineByName("JavaScript");

    // The library used to convert grammar rules to SVG.
    private static final String RAILROAD_SCRIPT = slurp(DiagramGenerator.class.getResourceAsStream("/railroad-diagram.js"));
    private static final String RAILROAD_CSS = slurp(DiagramGenerator.class.getResourceAsStream("/railroad-diagram.css"));

    // The templates used to create an HTML page from all grammar rules.
    private static final String HTML_TEMPLATE = slurp(DiagramGenerator.class.getResourceAsStream("/template.html"));
    private static final String CSS_TEMPLATE = slurp(DiagramGenerator.class.getResourceAsStream("/template.css"));

    // Initialize the JS engine to load the library used to convert the diagram-DSL
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

    // The ANTLR 4 grammar to parse. It can be a remote- or local file,
    // or even the grammar itself (as a string).
    private final String antlr4Grammar;

    // The filename of the ANTLR 4 grammar.
    private String antlr4GrammarFileName;

    // The grammar name of the grammar to parse.
    private String antlr4GrammarName;

    // The directory to save the html and/or png diagrams to.
    private File outputDir;

    // The collection that maps all grammar rules from `antlr4Grammar` to
    // a DSL that the JavaScript library, `railroad-diagram.js`, uses to
    // translate to SVG-railroad diagrams.
    private final Map<String, String> rules;

    /**
     * Creates a new instance of this class and will parse the
     * provided `antlr4Grammar`.
     *
     * @param antlr4Grammar
     *         the ANTLR 4 grammar to parse. It can be a remote- or local file,
     *         or even the grammar itself (as a string).
     *
     * @throws IOException
     *         when the grammar could not be parsed.
     */
    public DiagramGenerator(String antlr4Grammar) throws IOException {
        this.antlr4Grammar = antlr4Grammar.trim();
        this.antlr4GrammarFileName = null;
        this.antlr4GrammarName = null;
        this.outputDir = null;
        this.rules = parse();
    }

    /**
     * Parses `this.antlr4Grammar` and returns all parsed grammar rules.
     *
     * @return all parsed grammar rules.
     *
     * @throws IOException
     *         when the grammar could not be parsed.
     */
    private Map<String, String> parse() throws IOException {

        InputStream input;

        File file = new File(antlr4Grammar);

        // First check if `antlr4Grammar` is a local file.
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

        if (!this.outputDir.exists() && !this.outputDir.mkdirs()) {
            throw new RuntimeException("could not create output dir: " + this.outputDir);
        }

        // Now parse the grammar.
        ANTLRv4Lexer lexer = new ANTLRv4Lexer(new ANTLRInputStream(new BufferedInputStream(input)));
        ANTLRv4Parser parser = new ANTLRv4Parser(new CommonTokenStream(lexer));

        ParseTree tree = parser.grammarSpec();
        RuleVisitor visitor = new RuleVisitor();
        visitor.visit(tree);

        return visitor.getRules();
    }

    /**
     * Returns a map containing all parser- and lexer rules mapped to
     * the DSL `railroad-diagram.js` uses to translate to SVG-railroad
     * diagrams.
     *
     * @return a map containing all parser- and lexer rules mapped to
     * the DSL `railroad-diagram.js` uses to translate to SVG-railroad
     * diagrams.
     */
    public Map<String, String> getRules() {
        return new LinkedHashMap<String, String>(rules);
    }

    /**
     * Returns the SVG railroad diagram corresponding to the provided grammar rule.
     *
     * @param ruleName
     *         the grammar rule to get the SVG railroad diagram from.
     *
     * @return the SVG railroad diagram corresponding to the provided grammar rule.
     */
    public String getSVG(String ruleName) {

        try {
            CharSequence dsl = rules.get(ruleName);

            if (dsl == null) {
                throw new RuntimeException("no such rule found: " + ruleName);
            }

            // Evaluate the DSL that translates the input back to a SVG.
            String svg = (String) ENGINE.eval(dsl.toString());

            // Insert the proper namespaces and (custom) style sheet.
            svg = svg.replaceFirst("<svg ", "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ");
            svg = svg.replaceFirst("<g ", "<style type=\"text/css\">" + RAILROAD_CSS + "</style>\n<g ");

            return svg;
        }
        catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a PNG image from the provided grammar rule.
     *
     * @param ruleName
     *         the grammar rule to create a PNG image from.
     *
     * @return `true` iff the writing of the image was successful.
     */
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

    /**
     * create pdf file based on png images which are generated by provided grammar rule
     *
     * @param rules the grammar rule to get the pdf of railroad diagram from.
     * @return `true` if writting of the pdf is successful
     */
    public boolean createPdf(Map<String, String> rules) {
        if (rules == null) {
            return false;
        }

        if (rules.isEmpty()) {
            return false;
        }

        InputStream stream = null;
        try {
            Document convertPngToPdf = new Document();
            PdfWriter.getInstance(convertPngToPdf, new FileOutputStream(new File(this.outputDir, "index.pdf")));
            convertPngToPdf.open();
            for (String ruleName : rules.keySet()) {
                // get the png file just created
                stream = new FileInputStream(new File(this.outputDir, ruleName + ".png"));
                Image ruleImage = PngImage.getImage(stream);
                if (ruleImage == null) {
                    return false;
                }
                // set up the page layout
                float documentWidth =
                    convertPngToPdf.getPageSize().getWidth() - convertPngToPdf.leftMargin() - convertPngToPdf
                        .rightMargin();
                float documentHeight =
                    convertPngToPdf.getPageSize().getHeight() - convertPngToPdf.topMargin() - convertPngToPdf
                        .bottomMargin();
                // fit the image to the pdf page
                ruleImage.scaleToFit(documentWidth, documentHeight);
                // append the image to the pdf
                convertPngToPdf.add(new Paragraph(ruleName.concat(" : ")));
                convertPngToPdf.add(ruleImage);

            }
            convertPngToPdf.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (DocumentException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Creates an html page as a string of all grammar rules.
     *
     * @param fileName
     *         the name of the html page.
     *
     * @return an html page as a string of all grammar rules.
     */
    public String getHtml(String fileName) {

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

        return template;
    }

    /**
     * Creates a default (index.html) page containing all grammar rules.
     *
     * @return `true` iff the creation of the html page was successful.
     */
    public boolean createHtml() {
        return createHtml("index.html");
    }

    /**
     * Creates an html page containing all grammar rules.
     *
     * @param fileName
     *         the file name of the generated html page.
     *
     * @return `true` iff the creation of the html page was successful.
     */
    public boolean createHtml(String fileName) {

        String html = this.getHtml(fileName);

        PrintWriter out = null;

        try {
            out = new PrintWriter(new File(this.outputDir, fileName));
            out.write(html);
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

    /**
     * Converts an input stream into a String.
     *
     * @param input
     *         the input to convert into a String.
     *
     * @return the input stream as a String.
     */
    private static String slurp(InputStream input) {

        StringBuilder builder = new StringBuilder();
        Scanner scan = new Scanner(input);

        while (scan.hasNextLine()) {
            builder.append(scan.nextLine()).append(scan.hasNextLine() ? "\n" : "");
        }

        return builder.toString();
    }

    // TODO rewrite the stuff below to properly wrap <a ...> tags around the <text ...> tags that have a corresponding grammar rule in `this#rules`.

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
