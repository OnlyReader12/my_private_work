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

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for splitting HTML into tokens of tags and text.
 */
public class HtmlTokenizer {

    public static List<String> tokenize(String html) {
        List<String> tokens = new ArrayList<>();
        int pos = 0;
        String token = "";
        int len = html.length();
        while (pos < len) {
            char c = html.charAt(pos);

            String ahead = html.substring(pos, pos > len - 4 ? len : pos + 4);

            // a comment is starting
            if ("<!--".equals(ahead)) {
                // store the current token
                if (token.length() > 0) {
                    tokens.add(token);
                }

                // clear the token
                token = "";

                // search the end of <......>
                int end = moveToMarkerEnd(pos, "-->", html);
                tokens.add(html.substring(pos, end));
                pos = end;

                // a new "<" token is starting
            } else if ('<' == c) {

                // store the current token
                if (token.length() > 0) {
                    tokens.add(token);
                }

                // clear the token
                token = "";

                // serch the end of <......>
                int end = moveToMarkerEnd(pos, ">", html);
                tokens.add(html.substring(pos, end));
                pos = end;

            } else {
                token = token + c;
                pos++;
            }

        }

        // store the last token
        if (token.length() > 0) {
            tokens.add(token);
        }

        return tokens;
    }

    private static int moveToMarkerEnd(int pos, String marker, String s) {
        int i = s.indexOf(marker, pos);
        if (i > -1) {
            pos = i + marker.length();
        } else {
            pos = s.length();
        }
        return pos;
    }
}
