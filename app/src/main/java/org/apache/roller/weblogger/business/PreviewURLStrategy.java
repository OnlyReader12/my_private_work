/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
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

package org.apache.roller.weblogger.business;

import java.util.HashMap;
import java.util.Map;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogTheme;
import org.apache.roller.weblogger.util.URLUtilities;

/**
 * A URLStrategy used by the preview rendering system.
 * By extending MultiWeblogURLStrategy and overriding the base hooks,
 * we avoid duplicating the complex URL building logic.
 */
public class PreviewURLStrategy extends MultiWeblogURLStrategy {

    private final String previewTheme;
    private static final String PREVIEW_URL_SEGMENT = "/roller-ui/authoring/preview/";

    public PreviewURLStrategy(String theme) {
        previewTheme = theme;
    }

    @Override
    protected String getURLPrefix(Weblog weblog) {
        return PREVIEW_URL_SEGMENT + weblog.getHandle() + "/";
    }

    @Override
    protected Map<String, String> getPreviewParams() {
        Map<String, String> params = new HashMap<>();
        if (previewTheme != null) {
            params.put("theme", URLUtilities.encode(previewTheme));
        }
        return params;
    }

    /**
     * Preview entries need a specific parameter name instead of a path segment.
     */
    @Override
    public String getWeblogEntryURL(Weblog weblog,
            String locale,
            String previewAnchor,
            boolean absolute) {

        if (weblog == null) {
            return null;
        }

        StringBuilder url = getWeblogBaseURL(weblog, locale, absolute);

        Map<String, String> params = getPreviewParams();
        if (previewAnchor != null) {
            params.put("previewEntry", URLUtilities.encode(previewAnchor));
        }

        return url.append(URLUtilities.getQueryString(params)).toString();
    }

    /**
     * Preview resources have a slightly different path structure.
     */
    @Override
    public String getWeblogResourceURL(Weblog weblog, String filePath, boolean absolute) {

        if (weblog == null) {
            return null;
        }

        StringBuilder url = new StringBuilder(URL_BUFFER_SIZE);
        url.append(getContextURL(absolute));
        url.append("/roller-ui/authoring/previewresource/").append(weblog.getHandle()).append('/');

        if (filePath.startsWith("/")) {
            url.append(filePath.substring(1));
        } else {
            url.append(filePath);
        }

        Map<String, String> params = new HashMap<>();
        if (previewTheme != null && !WeblogTheme.CUSTOM.equals(previewTheme)) {
            params.put("theme", URLUtilities.encode(previewTheme));
        }

        return url.append(URLUtilities.getQueryString(params)).toString();
    }

}
