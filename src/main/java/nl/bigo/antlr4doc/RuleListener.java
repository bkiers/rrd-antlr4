package nl.bigo.antlr4doc;

import org.antlr.v4.runtime.misc.NotNull;

import java.util.HashMap;
import java.util.Map;

public class RuleListener extends ANTLRv4ParserBaseListener {

    private Map<String, String> rules;
    private StringBuilder builder;

    public RuleListener() {
        this.rules = new HashMap<String, String>();
    }

    public String getDiagram(String ruleName) {
        return this.rules.get(ruleName);
    }

    public Map<String, String> getRules() {
        return rules;
    }

    @Override
    public void enterLexerRule(@NotNull ANTLRv4Parser.LexerRuleContext ctx) {
        // TODO: DOC_COMMENT
        this.builder = new StringBuilder("Diagram(");
    }

    @Override
    public void exitLexerRule(@NotNull ANTLRv4Parser.LexerRuleContext ctx) {
        this.builder.append(").toString()");
        String ruleName = ctx.TOKEN_REF().getText();
        this.rules.put(ruleName, builder.toString().replaceAll("(?<=\\)),\\s*(?=\\))", ""));
    }

    @Override
    public void enterParserRuleSpec(@NotNull ANTLRv4Parser.ParserRuleSpecContext ctx) {
        // TODO: DOC_COMMENT
        this.builder = new StringBuilder("Diagram(");
    }

    @Override
    public void exitParserRuleSpec(@NotNull ANTLRv4Parser.ParserRuleSpecContext ctx) {
        this.builder.append(").toString()");
        String ruleName = ctx.RULE_REF().getText();
        this.rules.put(ruleName, builder.toString().replaceAll("(?<=\\)),\\s*(?=\\))", ""));
    }

    @Override
    public void enterLexerAltList(@NotNull ANTLRv4Parser.LexerAltListContext ctx) {
        this.builder.append("Choice(0, ");
    }

    @Override
    public void exitLexerAltList(@NotNull ANTLRv4Parser.LexerAltListContext ctx) {
        this.builder.append(")");
    }

    @Override
    public void enterLexerElements(@NotNull ANTLRv4Parser.LexerElementsContext ctx) {
        this.builder.append("Sequence(");
    }

    @Override
    public void exitLexerElements(@NotNull ANTLRv4Parser.LexerElementsContext ctx) {
        this.builder.append("), ");
    }

    @Override
    public void enterLexerAtom(@NotNull ANTLRv4Parser.LexerAtomContext ctx) {

        String text = ctx.getText();

        text = text.replace("'", "\\'");

        text = text.replace("\\u", "0x");

        this.builder.append("Terminal('").append(text).append("'), ");
    }
}
