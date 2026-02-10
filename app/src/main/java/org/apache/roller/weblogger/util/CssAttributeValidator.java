/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.roller.weblogger.util;

import org.apache.commons.validator.routines.UrlValidator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates and sanitizes the 'style' attribute.
 */
public class CssAttributeValidator implements AttributeValidator {

    private static final Pattern stylePattern = Pattern.compile("([^\\s^:]+)\\s*:\\s*([^;]+);?");
    private static final Pattern urlStylePattern = Pattern.compile("(?i).*\\b\\s*url\\s*\\(['\"]([^)]*)['\"]\\)");
    private static final Pattern forbiddenStylePattern = Pattern.compile("(?:(expression|eval|javascript))\\s*\\(");
    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[] { "http", "https" });

    @Override
    public String validate(String tag, String attr, String value, List<String> invalidTags) {
        if (!"style".equals(attr)) {
            return value;
        }

        Matcher styles = stylePattern.matcher(value);
        StringBuilder cleanStyle = new StringBuilder();

        while (styles.find()) {
            String styleName = styles.group(1).toLowerCase();
            String styleValue = styles.group(2);

            // suppress invalid styles values
            if (forbiddenStylePattern.matcher(styleValue).find()) {
                invalidTags.add(tag + " " + attr + " " + styleValue);
                continue;
            }

            // check if valid url
            Matcher urlStyleMatcher = urlStylePattern.matcher(styleValue);
            if (urlStyleMatcher.find()) {
                String url = urlStyleMatcher.group(1);
                if (!URL_VALIDATOR.isValid(url)) {
                    invalidTags.add(tag + " " + attr + " " + styleValue);
                    continue;
                }
            }

            cleanStyle.append(styleName).append(":").append(HTMLSanitizer.encode(styleValue)).append(";");
        }
        return cleanStyle.toString();
    }
}
