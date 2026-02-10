package org.apache.roller.weblogger.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.roller.weblogger.config.WebloggerConfig;

public final class HTMLSanitizer {

    private static final boolean XSS_ENABLED = WebloggerConfig.getBooleanProperty("weblogAdminsUntrusted",
            Boolean.FALSE);

    private static final Pattern FORBIDDEN_TAGS = Pattern.compile("^(script|object|embed|link|style|form|input)$");
    private static final Pattern ALLOWED_TAGS = Pattern.compile(
            "^(b|p|i|s|a|img|table|thead|tbody|tfoot|tr|th|td|dd|dl|dt|em|h1|h2|h3|h4|h5|h6|li|ul|ol|span|div|strike|strong|"
                    + "sub|sup|pre|del|code|blockquote|kbd|br|hr|area|map|object|embed|param|link|form|small|big)$");

    private static final Pattern COMMENT_PATTERN = Pattern.compile("<!--.*");
    private static final Pattern TAG_START_PATTERN = Pattern.compile("<(?i)(\\w+\\b)\\s*(.*)/?>$");
    private static final Pattern TAG_CLOSE_PATTERN = Pattern.compile("</(?i)(\\w+\\b)\\s*>$");
    private static final Pattern STAND_ALONE_TAGS = Pattern.compile("^(img|br|hr)$");
    private static final Pattern SELF_CLOSED = Pattern.compile("<.+/>");
    private static final Pattern ATTRIBUTES_PATTERN = Pattern.compile("(\\w*)\\s*=\\s*\"([^\"]*)\"");

    private static final Map<String, AttributeValidator> ATTRIBUTE_VALIDATORS;

    static {
        Map<String, AttributeValidator> validators = new HashMap<>();
        AttributeValidator urlValidator = new UrlAttributeValidator();
        validators.put("href", urlValidator);
        validators.put("src", urlValidator);

        AttributeValidator sizeValidator = new SizeAttributeValidator();
        validators.put("width", sizeValidator);
        validators.put("height", sizeValidator);

        validators.put("style", new CssAttributeValidator());
        ATTRIBUTE_VALIDATORS = Collections.unmodifiableMap(validators);
    }

    private HTMLSanitizer() {
        // Utility class
    }

    public static boolean isXssEnabled() {
        return XSS_ENABLED;
    }

    /**
     * This method should be used to test input.
     *
     * @param html
     * @return true if the input is "valid"
     */
    public static boolean isSanitized(String html) {
        return sanitizer(html).isValid();
    }

    /**
     * Used to clean every html before to output it in any html page
     *
     * @param html
     * @return sanitized html
     */
    public static String sanitize(String html) {
        return sanitizer(html).getHtml();
    }

    public static String conditionallySanitize(String ret) {
        // if XSS is enabled then sanitize HTML
        if (XSS_ENABLED && ret != null) {
            ret = HTMLSanitizer.sanitize(ret);
        }
        return ret;
    }

    /**
     * Used to get the text, tags removed or encoded
     *
     * @param html
     * @return sanitized text
     */
    public static String getText(String html) {
        return sanitizer(html).getText();
    }

    /**
     * This is the main method of sanitizing. It will be used both for validation
     * and cleaning
     *
     * @param html
     * @return a SanitizeResult object
     */
    public static SanitizeResult sanitizer(String html) {
        return sanitizer(html, ALLOWED_TAGS, FORBIDDEN_TAGS);
    }

    public static SanitizeResult sanitizer(String html, Pattern allowedTags, Pattern forbiddenTags) {
        SanitizeResult resultAccumulator = new SanitizeResult();
        Deque<String> openTags = new ArrayDeque<>();

        List<String> tokens = HtmlTokenizer.tokenize(html);

        for (String token : tokens) {
            processToken(token, allowedTags, forbiddenTags, openTags, resultAccumulator);
        }

        while (!openTags.isEmpty()) {
            String poppedTag = openTags.pop();
            String closeTag = "</" + poppedTag + ">";
            resultAccumulator.appendHtml(closeTag);
            resultAccumulator.appendVal(closeTag);
        }

        return resultAccumulator;
    }

    private static void processToken(String token, Pattern allowedTags, Pattern forbiddenTags,
            Deque<String> openTags, SanitizeResult resultAccumulator) {

        if (COMMENT_PATTERN.matcher(token).find()) {
            processComment(token, resultAccumulator);
            return;
        }

        Matcher startMatcher = TAG_START_PATTERN.matcher(token);
        if (startMatcher.find()) {
            processStartTag(token, startMatcher, allowedTags, forbiddenTags, openTags, resultAccumulator);
            return;
        }

        Matcher endMatcher = TAG_CLOSE_PATTERN.matcher(token);
        if (endMatcher.find()) {
            processEndTag(token, endMatcher, allowedTags, forbiddenTags, openTags, resultAccumulator);
            return;
        }

        finalizeToken(token, false, resultAccumulator);
    }

    private static void processComment(String token, SanitizeResult resultAccumulator) {
        String commentText = token + (token.endsWith("-->") ? "" : "-->");
        resultAccumulator.addInvalidTag(commentText);
        resultAccumulator.appendVal(commentText);
    }

    private static void processStartTag(String token, Matcher startMatcher, Pattern allowedTags,
            Pattern forbiddenTags, Deque<String> openTags, SanitizeResult resultAccumulator) {

        String tag = startMatcher.group(1).toLowerCase();

        if (forbiddenTags.matcher(tag).find()) {
            resultAccumulator.addInvalidTag("<" + tag + ">");
            return;
        }

        if (!allowedTags.matcher(tag).find()) {
            resultAccumulator.addInvalidTag(token);
            resultAccumulator.appendVal(token);
            return;
        }

        if (isTagMisplaced(tag, openTags)) {
            resultAccumulator.addInvalidTag("<" + tag + ">");
            return;
        }

        String tokenBody = startMatcher.group(2);
        StringBuilder cleanTokenBuilder = new StringBuilder("<").append(tag);
        boolean foundURL = false;
        boolean skipTag = false;

        Matcher attributes = ATTRIBUTES_PATTERN.matcher(tokenBody);
        while (attributes.find()) {
            String attr = attributes.group(1).toLowerCase();
            String val = attributes.group(2);

            if (attr.startsWith("on")) {
                resultAccumulator.addInvalidTag(tag + " " + attr + " " + val);
                continue;
            }

            AttributeValidator validator = ATTRIBUTE_VALIDATORS.get(attr);
            if (validator != null) {
                String validatedVal = validator.validate(tag, attr, val, resultAccumulator.getInvalidTags());
                if (validatedVal == null) {
                    skipTag = true;
                    break;
                }
                val = validatedVal;
                if (isUrlAttribute(tag, attr) && !val.isEmpty()) {
                    foundURL = true;
                }
            } else {
                val = encode(val);
            }
            cleanTokenBuilder.append(" ").append(attr).append("=\"").append(val).append("\"");
        }

        if (skipTag) {
            return;
        }

        cleanTokenBuilder.append(">");
        String cleanToken = cleanTokenBuilder.toString();
        boolean isAccepted = true;

        if (isUrlRequiredTag(tag) && !foundURL) {
            isAccepted = false;
            cleanToken = "";
        }

        if (isAccepted && !isStandAlone(tag)) {
            openTags.push(tag);
        }

        finalizeToken(cleanToken, isAccepted, resultAccumulator);
    }

    private static boolean isTagMisplaced(String tag, Deque<String> openTags) {
        if (isTableComponent(tag) && !openTags.contains("table")) {
            return true;
        }
        return isTableCell(tag) && !openTags.contains("tr");
    }

    private static void processEndTag(String token, Matcher endMatcher, Pattern allowedTags,
            Pattern forbiddenTags, Deque<String> openTags, SanitizeResult resultAccumulator) {

        String tag = endMatcher.group(1).toLowerCase();

        if (SELF_CLOSED.matcher(tag).find()) {
            resultAccumulator.addInvalidTag(token);
            return;
        }
        if (forbiddenTags.matcher(tag).find()) {
            resultAccumulator.addInvalidTag("/" + tag);
            return;
        }
        if (!allowedTags.matcher(tag).find()) {
            resultAccumulator.addInvalidTag(token);
            resultAccumulator.appendVal(token);
            return;
        }

        StringBuilder cleanTokenBuilder = new StringBuilder();
        if (openTags.contains(tag)) {
            while (!openTags.isEmpty()) {
                String poppedTag = openTags.pop();
                cleanTokenBuilder.append("</").append(poppedTag).append(">");
                if (poppedTag.equals(tag)) {
                    break;
                }
            }
            finalizeToken(cleanTokenBuilder.toString(), true, resultAccumulator);
        }
    }

    private static void finalizeToken(String token, boolean isAccepted, SanitizeResult resultAccumulator) {
        resultAccumulator.appendVal(token);
        if (isAccepted) {
            resultAccumulator.appendHtml(token);
        } else {
            String sanToken = htmlEncodeApexesAndTags(token);
            resultAccumulator.appendHtml(sanToken);
            resultAccumulator.appendText(htmlEncodeApexesAndTags(removeLineFeed(token)));
        }
    }

    private static boolean isTableComponent(String tag) {
        return "thead".equals(tag) || "tbody".equals(tag) || "tfoot".equals(tag) || "tr".equals(tag);
    }

    private static boolean isTableCell(String tag) {
        return "td".equals(tag) || "th".equals(tag);
    }

    private static boolean isUrlAttribute(String tag, String attr) {
        return ("a".equals(tag) && "href".equals(attr)) || (tag.matches("img|embed") && "src".equals(attr));
    }

    private static boolean isUrlRequiredTag(String tag) {
        return tag.matches("a|img|embed");
    }

    private static boolean isStandAlone(String tag) {
        return STAND_ALONE_TAGS.matcher(tag).find() || SELF_CLOSED.matcher(tag).find();
    }

    /**
     * Contains the sanitizing results.
     */
    public static final class SanitizeResult {
        private final StringBuilder htmlBuilder = new StringBuilder();
        private final StringBuilder textBuilder = new StringBuilder();
        private final StringBuilder valBuilder = new StringBuilder();
        private final List<String> invalidTags = new ArrayList<>();

        public String getHtml() {
            return htmlBuilder.toString();
        }

        public String getText() {
            return textBuilder.toString();
        }

        public String getVal() {
            return valBuilder.toString();
        }

        public boolean isValid() {
            return invalidTags.isEmpty();
        }

        public List<String> getInvalidTags() {
            return invalidTags;
        }

        void appendHtml(String s) {
            htmlBuilder.append(s);
        }

        void appendText(String s) {
            textBuilder.append(s);
        }

        void appendVal(String s) {
            valBuilder.append(s);
        }

        void addInvalidTag(String s) {
            invalidTags.add(s);
        }
    }

    public static String encode(String s) {
        return convertLineFeedToBR(htmlEncodeApexesAndTags(s == null ? "" : s));
    }

    public static String htmlEncodeApexesAndTags(String source) {
        return htmlEncodeTag(htmlEncodeApexes(source));
    }

    public static String htmlEncodeApexes(String source) {
        if (source != null) {
            return replaceAllNoRegex(source, new String[] { "\"", "'" }, new String[] { "&quot;", "&#39;" });
        } else {
            return null;
        }
    }

    public static String htmlEncodeTag(String source) {
        if (source != null) {
            return replaceAllNoRegex(source, new String[] { "<", ">" }, new String[] { "&lt;", "&gt;" });
        } else {
            return null;
        }
    }

    public static String convertLineFeedToBR(String text) {
        if (text != null) {
            return replaceAllNoRegex(text, new String[] { "\n", "\f", "\r" }, new String[] { "<br>", "<br>", " " });
        } else {
            return null;
        }
    }

    public static String removeLineFeed(String text) {
        if (text != null) {
            return replaceAllNoRegex(text, new String[] { "\n", "\f", "\r" }, new String[] { " ", " ", " " });
        } else {
            return null;
        }
    }

    public static String replaceAllNoRegex(String source, String[] searches, String[] replaces) {
        String tmp = source;
        for (int k = 0; k < searches.length; k++) {
            tmp = replaceAllNoRegex(tmp, searches[k], replaces[k]);
        }
        return tmp;
    }

    public static String replaceAllNoRegex(String source, String search, String replace) {
        if (source == null) {
            return null;
        }
        if (search.isEmpty()) {
            return source;
        }

        StringBuilder buffer = new StringBuilder();
        int oldPos = 0;
        int pos = source.indexOf(search, oldPos);

        while (pos != -1) {
            buffer.append(source, oldPos, pos);
            buffer.append(replace);
            oldPos = pos + search.length();
            pos = source.indexOf(search, oldPos);
        }

        if (oldPos < source.length()) {
            buffer.append(source.substring(oldPos));
        }
        return buffer.toString();
    }
}
