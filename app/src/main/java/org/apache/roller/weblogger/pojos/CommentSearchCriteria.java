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
package org.apache.roller.weblogger.pojos;

import org.apache.roller.weblogger.pojos.WeblogEntryComment.ApprovalStatus;

public class CommentSearchCriteria extends DateRangeSearchCriteria {

    // Entry or null to include all comments
    private WeblogEntry entry;

    // Text appearing in comment, or null for all
    private String searchText;
    // Comment status as defined in WeblogEntryComment, or null for any
    private ApprovalStatus status;
    // True for results in reverse chrono order
    private boolean reverseChrono = false;

    public WeblogEntry getEntry() {
        return entry;
    }

    public void setEntry(WeblogEntry entry) {
        this.entry = entry;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public boolean isReverseChrono() {
        return reverseChrono;
    }

    public void setReverseChrono(boolean reverseChrono) {
        this.reverseChrono = reverseChrono;
    }

}
