package iped.engine.task.regex.validator;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import iped.engine.task.regex.BasicAbstractRegexValidatorService;

public class IPLRegexValidatorService extends BasicAbstractRegexValidatorService {

    private static final String REPLACEMENT = " ";

    private static final String REGEX_NAME = "IPL";

    private final Pattern NOT_ALLOWED_CHARS_PATTERN = Pattern.compile("[^IPLRE0-9\\-]");

    @Override
    public boolean validate(String hit) {
        return true;
    }

    @Override
    public String format(String hit) {
        Matcher matcher = NOT_ALLOWED_CHARS_PATTERN.matcher(hit);
        return matcher.replaceAll(REPLACEMENT).trim();
    }

    @Override
    public List<String> getRegexNames() {
        return Arrays.asList(REGEX_NAME);
    }

    @Override
    public void init(File confDir) {
        // TODO Auto-generated method stub

    }

}
