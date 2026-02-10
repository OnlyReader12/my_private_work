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

import java.util.List;
import org.apache.roller.weblogger.pojos.WeblogEntry.PubStatus;

public class WeblogEntrySearchCriteria extends DateRangeSearchCriteria {

    public enum SortOrder {
        ASCENDING, DESCENDING
    }

    public enum SortBy {
        PUBLICATION_TIME, UPDATE_TIME
    }

    // TODO: See if can switch from name of Category to Category object

    // User or null to get for all users.
    private User user;
    // Category name or null for all categories.
    private String catName;
    // If provided, array of tags to search blog entries for, just one needs to
    // match to retrieve entry
    private List<String> tags;
    // Publication status of the weblog entry (DRAFT, PUBLISHED, etc.)
    private PubStatus status;
    // Text appearing in the text or summary, or null for all
    private String text;
    // Date field to sort by
    private SortBy sortBy = SortBy.PUBLICATION_TIME;
    // Order of sort
    private SortOrder sortOrder = SortOrder.DESCENDING;

    private String locale;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCatName() {
        return catName;
    }

    public void setCatName(String catName) {
        this.catName = catName;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public PubStatus getStatus() {
        return status;
    }

    public void setStatus(PubStatus status) {
        this.status = status;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public void setSortBy(SortBy sortBy) {
        this.sortBy = sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

}
