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

package org.apache.roller.weblogger.pojos;

/**
 * Represents the search criteria for Lucene search operations.
 */
public class LuceneSearchCriteria extends BaseSearchCriteria {

    private String term;
    private String weblogHandle;
    private String categoryName;
    private String locale;

    public LuceneSearchCriteria() {
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getWeblogHandle() {
        return weblogHandle;
    }

    public void setWeblogHandle(String weblogHandle) {
        this.weblogHandle = weblogHandle;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public String toString() {
        return "LuceneSearchCriteria{" +
                "term='" + term + '\'' +
                ", weblogHandle='" + weblogHandle + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", locale='" + locale + '\'' +
                ", offset=" + getOffset() +
                ", maxResults=" + getMaxResults() +
                '}';
    }
}
