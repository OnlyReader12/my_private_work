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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HTMLSanitizerTest {

    @Test
    public void testSanitizeBasic() {
        assertEquals("<p>Hello World</p>", HTMLSanitizer.sanitize("<p>Hello World</p>"));
        assertEquals("<b>Bold</b>", HTMLSanitizer.sanitize("<b>Bold</b>"));
    }

    @Test
    public void testForbiddenTags() {
        // Forbidden tags are removed if well-formed
        String result = HTMLSanitizer.sanitize("<script>alert('xss')</script>");
        assertFalse(result.contains("<script>"), "Script tag should not be present");
        assertFalse(result.contains("&lt;script&gt;"), "Well-formed forbidden tag should be removed, not encoded");
        // Content is preserved but encoded
        assertTrue(result.contains("alert("), "Core content should be preserved");
    }

    @Test
    public void testUrlValidation() {
        // Valid URL
        assertEquals("<a href=\"http://example.com\">Link</a>",
                HTMLSanitizer.sanitize("<a href=\"http://example.com\">Link</a>"));

        // Invalid URL should result in tag being stripped (per isUrlRequiredTag logic)
        String result = HTMLSanitizer.sanitize("<a href=\"javascript:alert(1)\">Link</a>");
        assertEquals("Link", result.trim());

        // Mailto
        assertEquals("<a href=\"mailto:user@example.com\">Mail</a>",
                HTMLSanitizer.sanitize("<a href=\"mailto:user@example.com\">Mail</a>"));
    }

    @Test
    public void testCssValidation() {
        // Valid style
        assertEquals("<p style=\"color:red;\">Text</p>",
                HTMLSanitizer.sanitize("<p style=\"color:red\">Text</p>"));

        // Forbidden style component (expression)
        String result = HTMLSanitizer.sanitize("<p style=\"color:red; width:expression(alert(1))\">Text</p>");
        assertEquals("<p style=\"color:red;\">Text</p>", result);
    }

    @Test
    public void testUnclosedTags() {
        assertEquals("<p>Text</p>", HTMLSanitizer.sanitize("<p>Text"));
    }

    @Test
    public void testTableConsistency() {
        // tr outside table should be dropped/encoded
        String result = HTMLSanitizer.sanitize("<tr><td>Cell</td></tr>");
        assertFalse(result.contains("<tr>"), "TR outside table should be stripped");
    }
}
