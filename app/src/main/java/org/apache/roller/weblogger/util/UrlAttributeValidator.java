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

/**
 * Validates URL attributes like href and src.
 */
public class UrlAttributeValidator implements AttributeValidator {

    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[] { "http", "https" });

    @Override
    public String validate(String tag, String attr, String value, List<String> invalidTags) {
        if ("a".equals(tag) && "href".equals(attr)) {
            if (URL_VALIDATOR.isValid(value)) {
                return value;
            } else {
                // may be it is a mailto?
                // case <a href="mailto:pippo@pippo.com?subject=...."
                if (value.toLowerCase().startsWith("mailto:") && value.indexOf('@') >= 0) {
                    String val1 = "http://www." + value.substring(value.indexOf('@') + 1);
                    if (URL_VALIDATOR.isValid(val1)) {
                        return value;
                    }
                }
            }
        } else if (tag.matches("img|embed") && "src".equals(attr)) {
            if (URL_VALIDATOR.isValid(value)) {
                return value;
            }
        } else if ("href".equals(attr) || "src".equals(attr)) {
            // For any other tag, href/src are not allowed by this logic (skipped in
            // original)
            invalidTags.add(tag + " " + attr + " " + value);
            return null; // Return null to indicate "continue" in the loop or skip
        }

        invalidTags.add(attr + " " + value);
        return "";
    }
}
