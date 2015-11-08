import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static nl.bigo.rrdantlr4.CommentsParser.*;
import static org.fest.assertions.Assertions.assertThat;

public class CommentsParserTest {
    public static final String RULE_ONE = "/* <RULE_NAME> RULE_DESCRIPTION*/";
    public static final String RULE_TWO = "/* <RULE2_NAME> RULE2_DESCRIPTION*/";

    @Test
    public void it_should_find_the_rule_name_of_a_comment() {
        Optional<String> ruleName = ruleName(RULE_ONE);
        assertThat(ruleName).isEqualTo(Optional.of("RULE_NAME"));
    }

    @Test
    public void it_should_return_an_absent_string_when_the_rule_name_is_missing() {
        Optional<String> ruleName = ruleName("/*RULE_DESCRIPTION*/");
        assertThat(ruleName).isEqualTo(Optional.<String>absent());
    }

    @Test
    public void it_should_find_the_rule_description_of_a_comment() {
        String ruleName = ruleDescription(RULE_ONE);
        assertThat(ruleName).isEqualTo("RULE_DESCRIPTION");
    }

    @Test
    public void it_should_find_the_comments_from_a_string_input() {
        List<String> comments = comments(RULE_ONE + " HELLO " + RULE_TWO);
        assertThat(comments).containsOnly(RULE_ONE, RULE_TWO);
    }

    @Test
    public void it_should_return_an_empty_list_when_no_comment_pattern_is_found() {
        List<String> comments = comments(" HELLO ");
        assertThat(comments).isEmpty();
    }

    @Test
    public void it_should_return_a_map_of_rule_name_to_description() {
        Map<String, String> ruleNameToDescription = commentsMap(RULE_ONE + " HELLO " + RULE_TWO);
        assertThat(ruleNameToDescription).isEqualTo(ImmutableMap.of("RULE_NAME", "RULE_DESCRIPTION", "RULE2_NAME", "RULE2_DESCRIPTION"));
    }

    @Test
    public void it_should_ignore_comments_without_rule_names() {
        Map<String, String> ruleNameToDescription = commentsMap(RULE_ONE + " HELLO /*RULE_DESCRIPTION*/");
        assertThat(ruleNameToDescription).isEqualTo(ImmutableMap.of("RULE_NAME", "RULE_DESCRIPTION"));
    }

    @Test
    public void it_should_parse_a_text_with_new_lines() {
        Map<String, String> ruleNameToDescription = commentsMap("/*"

            + "<RULE_NAME> RULE_DESCRIPTION \n"

            + "*/");
        assertThat(ruleNameToDescription).isEqualTo(ImmutableMap.of("RULE_NAME", "RULE_DESCRIPTION"));
    }

}
