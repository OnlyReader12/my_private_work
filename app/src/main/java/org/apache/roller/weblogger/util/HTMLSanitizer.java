package org.apache.roller.weblogger.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.roller.weblogger.config.WebloggerConfig;

public class HTMLSanitizer {
    public static Boolean xssEnabled = WebloggerConfig.getBooleanProperty("weblogAdminsUntrusted", Boolean.FALSE);

    public static Pattern forbiddenTags = Pattern.compile("^(script|object|embed|link|style|form|input)$");
    public static Pattern allowedTags = Pattern.compile(
            "^(b|p|i|s|a|img|table|thead|tbody|tfoot|tr|th|td|dd|dl|dt|em|h1|h2|h3|h4|h5|h6|li|ul|ol|span|div|strike|strong|"
                    + "sub|sup|pre|del|code|blockquote|kbd|br|hr|area|map|object|embed|param|link|form|small|big)$");

    private static final Pattern commentPattern = Pattern.compile("<!--.*");
    private static final Pattern tagStartPattern = Pattern.compile("<(?i)(\\w+\\b)\\s*(.*)/?>$");
    private static final Pattern tagClosePattern = Pattern.compile("</(?i)(\\w+\\b)\\s*>$");
    private static final Pattern standAloneTags = Pattern.compile("^(img|br|hr)$");
    private static final Pattern selfClosed = Pattern.compile("<.+/>");
    private static final Pattern attributesPattern = Pattern.compile("(\\w*)\\s*=\\s*\"([^\"]*)\"");

    private static final Map<String, AttributeValidator> attributeValidators = new HashMap<>();

    static {
        AttributeValidator urlValidator = new UrlAttributeValidator();
        attributeValidators.put("href", urlValidator);
        attributeValidators.put("src", urlValidator);

        AttributeValidator sizeValidator = new SizeAttributeValidator();
        attributeValidators.put("width", sizeValidator);
        attributeValidators.put("height", sizeValidator);

        attributeValidators.put("style", new CssAttributeValidator());
    }

    /**
     * This method should be used to test input.
     *
     * @param html
     * @return true if the input is "valid"
     */
    public static boolean isSanitized(String html) {
        return sanitizer(html).isValid;
    }

    /**
     * Used to clean every html before to output it in any html page
     *
     * @param html
     * @return sanitized html
     */
    public static String sanitize(String html) {
        return sanitizer(html).html;
    }

    public static String conditionallySanitize(String ret) {
        // if XSS is enabled then sanitize HTML
        if (xssEnabled && ret != null) {
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
        return sanitizer(html).text;
    }

    /**
     * This is the main method of sanitizing. It will be used both for validation
     * and cleaning
     *
     * @param html
     * @return a SanitizeResult object
     */
    public static SanitizeResult sanitizer(String html) {
        return sanitizer(html, allowedTags, forbiddenTags);
    }

    public static SanitizeResult sanitizer(String html, Pattern allowedTags, Pattern forbiddenTags) {
        SanitizeResult ret = new SanitizeResult();
        Stack<String> openTags = new Stack<>();

        List<String> tokens = HtmlTokenizer.tokenize(html);

        for (String token : tokens) {
            boolean isAcceptedToken = false;

            Matcher startMatcher = tagStartPattern.matcher(token);
            Matcher endMatcher = tagClosePattern.matcher(token);

            if (commentPattern.matcher(token).find()) {
                ret.val = ret.val + token + (token.endsWith("-->") ? "" : "-->");
                ret.invalidTags.add(token + (token.endsWith("-->") ? "" : "-->"));
                continue;
            } else if (startMatcher.find()) {
                String tag = startMatcher.group(1).toLowerCase();

                if (forbiddenTags.matcher(tag).find()) {
                    ret.invalidTags.add("<" + tag + ">");
                    continue;
                } else if (allowedTags.matcher(tag).find()) {
                    String cleanToken = "<" + tag;
                    String tokenBody = startMatcher.group(2);

                    if (isTableComponent(tag)) {
                        if (openTags.search("table") < 1) {
                            ret.invalidTags.add("<" + tag + ">");
                            continue;
                        }
                    } else if (isTableCell(tag) && openTags.search("tr") < 1) {
                        ret.invalidTags.add("<" + tag + ">");
                        continue;
                    }

                    Matcher attributes = attributesPattern.matcher(tokenBody);
                    boolean foundURL = false;
                    boolean skipTag = false;

                    while (attributes.find()) {
                        String attr = attributes.group(1).toLowerCase();
                        String val = attributes.group(2);

                        if (attr.startsWith("on")) {
                            ret.invalidTags.add(tag + " " + attr + " " + val);
                            continue;
                        }

                        AttributeValidator validator = attributeValidators.get(attr);
                        if (validator != null) {
                            String validatedVal = validator.validate(tag, attr, val, ret.invalidTags);
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

                        cleanToken = cleanToken + " " + attr + "=\"" + val + "\"";
                    }

                    if (skipTag)
                        continue;

                    cleanToken = cleanToken + ">";
                    isAcceptedToken = true;

                    if (isUrlRequiredTag(tag) && !foundURL) {
                        isAcceptedToken = false;
                        cleanToken = "";
                    }

                    token = cleanToken;

                    if (isAcceptedToken && !isStandAlone(tag)) {
                        openTags.push(tag);
                    }
                } else {
                    ret.invalidTags.add(token);
                    ret.val = ret.val + token;
                    continue;
                }
            } else if (endMatcher.find()) {
                String tag = endMatcher.group(1).toLowerCase();

                if (selfClosed.matcher(tag).find()) {
                    ret.invalidTags.add(token);
                    continue;
                }
                if (forbiddenTags.matcher(tag).find()) {
                    ret.invalidTags.add("/" + tag);
                    continue;
                }
                if (!allowedTags.matcher(tag).find()) {
                    ret.invalidTags.add(token);
                    ret.val = ret.val + token;
                    continue;
                } else {
                    String cleanToken = "";
                    int pos = openTags.search(tag);
                    for (int i = 1; i <= pos; i++) {
                        String poppedTag = openTags.pop();
                        cleanToken = cleanToken + "</" + poppedTag + ">";
                        isAcceptedToken = true;
                    }
                    token = cleanToken;
                }
            }

            ret.val = ret.val + token;
            if (isAcceptedToken) {
                ret.html = ret.html + token;
            } else {
                String sanToken = htmlEncodeApexesAndTags(token);
                ret.html = ret.html + sanToken;
                ret.text = ret.text + htmlEncodeApexesAndTags(removeLineFeed(token));
            }
        }

        while (!openTags.isEmpty()) {
            String poppedTag = openTags.pop();
            ret.html = ret.html + "</" + poppedTag + ">";
            ret.val = ret.val + "</" + poppedTag + ">";
        }

        ret.isValid = ret.invalidTags.isEmpty();
        return ret;
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
        return standAloneTags.matcher(tag).find() || selfClosed.matcher(tag).find();
    }

    /**
     * Contains the sanitizing results.
     */
    public static class SanitizeResult {
        public String html = "";
        public String text = "";
        public String val = "";
        public boolean isValid = true;
        public List<String> invalidTags = new ArrayList<>();
    }

    public static String encode(String s) {
        return convertLineFeedToBR(htmlEncodeApexesAndTags(s == null ? "" : s));
    }

    public static final String htmlEncodeApexesAndTags(String source) {
        return htmlEncodeTag(htmlEncodeApexes(source));
    }

    public static final String htmlEncodeApexes(String source) {
        if (source != null) {
            return replaceAllNoRegex(source, new String[] { "\"", "'" }, new String[] { "&quot;", "&#39;" });
        } else {
            return null;
        }
    }

    public static final String htmlEncodeTag(String source) {
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

    public static final String replaceAllNoRegex(String source, String searches[], String replaces[]) {
        int k;
        String tmp = source;
        for (k = 0; k < searches.length; k++) {
            tmp = replaceAllNoRegex(tmp, searches[k], replaces[k]);
        }
        return tmp;
    }

    public static final String replaceAllNoRegex(String source, String search, String replace) {
        StringBuilder buffer = new StringBuilder();
        if (source != null) {
            if (search.length() == 0) {
                return source;
            }
            int oldPos, pos;
            for (oldPos = 0, pos = source.indexOf(search, oldPos); pos != -1; oldPos = pos
                    + search.length(), pos = source.indexOf(search, oldPos)) {
                buffer.append(source.substring(oldPos, pos));
                buffer.append(replace);
            }
            if (oldPos < source.length()) {
                buffer.append(source.substring(oldPos));
            }
        }
        return new String(buffer);
    }
}
