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
package org.apache.roller.weblogger.ui.struts2.editor;

import java.util.Arrays;

import java.util.ResourceBundle;
import org.apache.commons.lang3.StringUtils;
import org.apache.roller.util.RollerConstants;
import org.apache.roller.weblogger.pojos.MediaFileFilter;
import org.apache.roller.weblogger.pojos.MediaFileType;
import org.apache.roller.weblogger.pojos.MediaFileFilter.MediaFileOrder;
import org.apache.roller.weblogger.pojos.MediaFileFilter.SizeFilterType;

/**
 * Bean for holding media file search criteria.
 */
public class MediaFileSearchBean {
    private ResourceBundle bundle = ResourceBundle.getBundle("ApplicationResources");

    public static final int PAGE_SIZE = 10;

    // Media file name as search criteria
    private String name;

    // Media file type as search criteria
    private String type;

    // Type of size filter as search criteria
    private String sizeFilterType;

    // Size of file as search criteria
    private long size;

    // Size unit
    private String sizeUnit;

    // Tags as search criteria
    private String tags;

    // Page number of results
    private int pageNum = 0;

    // Sort option for search results
    private int sortOption;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        return this.bundle.getString(type);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSizeFilterType() {
        return sizeFilterType;
    }

    public void setSizeFilterType(String sizeFilterType) {
        this.sizeFilterType = sizeFilterType;
    }

    public String getSizeFilterTypeLabel() {
        return this.bundle.getString(sizeFilterType);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getSizeUnit() {
        return sizeUnit;
    }

    public String getSizeUnitLabel() {
        return this.bundle.getString(sizeUnit);
    }

    public void setSizeUnit(String sizeUnit) {
        this.sizeUnit = sizeUnit;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getSortOption() {
        return sortOption;
    }

    public void setSortOption(int sortOption) {
        this.sortOption = sortOption;
    }

    /**
     * Copies data from this bean to media file filter object.
     *
     */
    /**
     * Copies data from this bean to media file filter object.
     *
     */
    public void copyTo(MediaFileFilter dataHolder) {
        dataHolder.setName(this.name);
        dataHolder.setType(mapType(this.type));

        if (this.size > 0) {
            dataHolder.setSizeFilterType(mapObjectSizeFilterType(this.sizeFilterType));
            dataHolder.setSize(calculateFilterSize(this.size, this.sizeUnit));
        }

        if (!StringUtils.isEmpty(this.tags)) {
            dataHolder.setTags(Arrays.asList(this.tags.split(" ")));
        }

        dataHolder.setOffset(pageNum * PAGE_SIZE);
        dataHolder.setMaxResults(PAGE_SIZE + 1);
        dataHolder.setOrder(mapOrder(this.sortOption));
    }

    private MediaFileType mapType(String type) {
        if (StringUtils.isEmpty(type)) {
            return null;
        }
        if ("mediaFileView.audio".equals(type)) {
            return MediaFileType.AUDIO;
        } else if ("mediaFileView.video".equals(type)) {
            return MediaFileType.VIDEO;
        } else if ("mediaFileView.image".equals(type)) {
            return MediaFileType.IMAGE;
        } else if ("mediaFileView.others".equals(type)) {
            return MediaFileType.OTHERS;
        }
        return null;
    }

    private SizeFilterType mapObjectSizeFilterType(String sizeFilterType) {
        if ("mediaFileView.gt".equals(sizeFilterType)) {
            return SizeFilterType.GT;
        } else if ("mediaFileView.ge".equals(sizeFilterType)) {
            return SizeFilterType.GTE;
        } else if ("mediaFileView.eq".equals(sizeFilterType)) {
            return SizeFilterType.EQ;
        } else if ("mediaFileView.le".equals(sizeFilterType)) {
            return SizeFilterType.LTE;
        } else if ("mediaFileView.lt".equals(sizeFilterType)) {
            return SizeFilterType.LT;
        }
        return SizeFilterType.EQ;
    }

    private long calculateFilterSize(long size, String sizeUnit) {
        if ("mediaFileView.kb".equals(sizeUnit)) {
            return size * RollerConstants.ONE_KB_IN_BYTES;
        } else if ("mediaFileView.mb".equals(sizeUnit)) {
            return size * RollerConstants.ONE_MB_IN_BYTES;
        }
        return size;
    }

    private MediaFileOrder mapOrder(int sortOption) {
        switch (sortOption) {
            case 0:
                return MediaFileOrder.NAME;
            case 1:
                return MediaFileOrder.DATE_UPLOADED;
            case 2:
                return MediaFileOrder.TYPE;
            default:
                return null;
        }
    }
}
