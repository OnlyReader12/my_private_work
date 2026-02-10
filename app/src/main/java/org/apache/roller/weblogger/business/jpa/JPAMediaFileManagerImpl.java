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
package org.apache.roller.weblogger.business.jpa;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.imageio.ImageIO;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.FileContentManager;
import org.apache.roller.weblogger.business.FileIOException;
import org.apache.roller.weblogger.business.MediaFileManager;
import org.apache.roller.weblogger.business.WeblogManager;
import org.apache.roller.weblogger.business.Weblogger;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.pojos.FileContent;
import org.apache.roller.weblogger.pojos.MediaFile;
import org.apache.roller.weblogger.pojos.MediaFileDirectory;
import org.apache.roller.weblogger.pojos.MediaFileFilter;
import org.apache.roller.weblogger.pojos.MediaFileTag;
import org.apache.roller.weblogger.pojos.MediaFileType;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.util.RollerMessages;
import org.apache.roller.weblogger.util.Utilities;

@com.google.inject.Singleton
public class JPAMediaFileManagerImpl implements MediaFileManager {

    private final Weblogger roller;
    private final JPAPersistenceStrategy strategy;
    private static final Log log = LogFactory.getFactory().getInstance(JPAMediaFileManagerImpl.class);
    public static final String MIGRATION_STATUS_FILENAME = "migration-status.properties";
    public static final String DEFAULT_DIRECTORY_NAME = "default";

    /**
     * Creates a new instance of MediaFileManagerImpl
     */
    @com.google.inject.Inject
    protected JPAMediaFileManagerImpl(Weblogger roller,
            JPAPersistenceStrategy persistenceStrategy) {
        this.roller = roller;
        this.strategy = persistenceStrategy;
    }

    /**
     * Initialize manager; deal with upgrade/migration if 'uploads.migrate.auto'
     * is true.
     */
    @Override
    public void initialize() {
        boolean autoUpgrade = WebloggerConfig
                .getBooleanProperty("uploads.migrate.auto");
        if (autoUpgrade && this.isFileStorageUpgradeRequired()) {
            this.upgradeFileStorage();
        }
    }

    /**
     * Release resources; currently a no-op.
     */
    @Override
    public void release() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveMediaFiles(Collection<MediaFile> mediaFiles,
            MediaFileDirectory targetDirectory) throws WebloggerException {

        List<MediaFile> moved = new ArrayList<>(mediaFiles);

        for (MediaFile mediaFile : moved) {
            mediaFile.getDirectory().getMediaFiles().remove(mediaFile);

            mediaFile.setDirectory(targetDirectory);
            this.strategy.store(mediaFile);

            targetDirectory.getMediaFiles().add(mediaFile);
            this.strategy.store(targetDirectory);
        }
        // update weblog last modified date. date updated by saveWebsite()
        roller.getWeblogManager().saveWeblog(targetDirectory.getWeblog());

        // Refresh associated parent for changes
        roller.flush();
        if (!moved.isEmpty()) {
            strategy.refresh(moved.get(0).getDirectory());
        }

        // Refresh associated parent for changes
        strategy.refresh(targetDirectory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveMediaFile(MediaFile mediaFile,
            MediaFileDirectory targetDirectory) throws WebloggerException {
        moveMediaFiles(Arrays.asList(mediaFile), targetDirectory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createMediaFileDirectory(MediaFileDirectory directory)
            throws WebloggerException {
        this.strategy.store(directory);

        // update weblog last modified date. date updated by saveWebsite()
        roller.getWeblogManager().saveWeblog(directory.getWeblog());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaFileDirectory createMediaFileDirectory(Weblog weblog,
            String requestedName) throws WebloggerException {

        requestedName = requestedName.startsWith("/") ? requestedName.substring(1) : requestedName;

        if (requestedName.isEmpty() || requestedName.equals(DEFAULT_DIRECTORY_NAME)) {
            // Default cannot be created using this method.
            // Use createDefaultMediaFileDirectory instead
            throw new WebloggerException("Invalid name!");
        }

        MediaFileDirectory newDirectory;

        if (weblog.hasMediaFileDirectory(requestedName)) {
            throw new WebloggerException("Directory exists");
        } else {
            newDirectory = new MediaFileDirectory(weblog, requestedName, null);
            log.debug("Created new Directory " + requestedName);
        }

        // update weblog last modified date. date updated by saveWeblog()
        roller.getWeblogManager().saveWeblog(weblog);

        return newDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaFileDirectory createDefaultMediaFileDirectory(Weblog weblog)
            throws WebloggerException {
        MediaFileDirectory defaultDirectory = new MediaFileDirectory(weblog, DEFAULT_DIRECTORY_NAME,
                "default directory");
        createMediaFileDirectory(defaultDirectory);
        return defaultDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createMediaFile(Weblog weblog, MediaFile mediaFile,
            RollerMessages errors) throws WebloggerException {

        FileContentManager cmgr = WebloggerFactory.getWeblogger()
                .getFileContentManager();
        if (!cmgr.canSave(weblog, mediaFile.getName(),
                mediaFile.getContentType(), mediaFile.getLength(), errors)) {
            return;
        }
        strategy.store(mediaFile);

        // Refresh associated parent for changes
        roller.flush();
        strategy.refresh(mediaFile.getDirectory());

        // update weblog last modified date. date updated by saveWeblog()
        roller.getWeblogManager().saveWeblog(weblog);

        cmgr.saveFileContent(weblog, mediaFile.getId(),
                mediaFile.getInputStream());

        if (mediaFile.isImageFile()) {
            updateThumbnail(mediaFile);
        }
    }

    @Override
    public void createThemeMediaFile(Weblog weblog, MediaFile mediaFile,
            RollerMessages errors) throws WebloggerException {

        FileContentManager cmgr = WebloggerFactory.getWeblogger().getFileContentManager();
        strategy.store(mediaFile);

        // Refresh associated parent for changes
        roller.flush();
        strategy.refresh(mediaFile.getDirectory());

        // update weblog last modified date. date updated by saveWeblog()
        roller.getWeblogManager().saveWeblog(weblog);

        cmgr.saveFileContent(weblog, mediaFile.getId(),
                mediaFile.getInputStream());

        if (mediaFile.isImageFile()) {
            updateThumbnail(mediaFile);
        }
    }

    private void updateThumbnail(MediaFile mediaFile) {
        try {
            FileContentManager cmgr = WebloggerFactory.getWeblogger()
                    .getFileContentManager();
            FileContent fc = cmgr.getFileContent(mediaFile.getWeblog(),
                    mediaFile.getId());
            BufferedImage img;

            img = ImageIO.read(fc.getInputStream());

            // determine and save width and height
            mediaFile.setWidth(img.getWidth());
            mediaFile.setHeight(img.getHeight());
            strategy.store(mediaFile);

            int newWidth = mediaFile.getThumbnailWidth();
            int newHeight = mediaFile.getThumbnailHeight();

            // create thumbnail image
            Image newImage = img.getScaledInstance(newWidth, newHeight,
                    Image.SCALE_SMOOTH);
            BufferedImage tmp = new BufferedImage(newWidth, newHeight,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            g2.drawImage(newImage, 0, 0, newWidth, newHeight, null);
            g2.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(tmp, "png", baos);

            cmgr.saveFileContent(mediaFile.getWeblog(), mediaFile.getId()
                    + "_sm", new ByteArrayInputStream(baos.toByteArray()));

            roller.flush();
            // Refresh associated parent for changes
            strategy.refresh(mediaFile.getDirectory());

        } catch (Exception e) {
            log.debug("ERROR creating thumbnail", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMediaFile(Weblog weblog, MediaFile mediaFile)
            throws WebloggerException {
        mediaFile.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        strategy.store(mediaFile);

        roller.flush();
        // Refresh associated parent for changes
        strategy.refresh(mediaFile.getDirectory());

        // update weblog last modified date. date updated by saveWeblog()
        roller.getWeblogManager().saveWeblog(weblog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMediaFile(Weblog weblog, MediaFile mediaFile,
            InputStream is) throws WebloggerException {
        mediaFile.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        strategy.store(mediaFile);

        roller.flush();
        // Refresh associated parent for changes
        strategy.refresh(mediaFile.getDirectory());

        // update weblog last modified date. date updated by saveWeblog()
        roller.getWeblogManager().saveWeblog(weblog);

        FileContentManager cmgr = WebloggerFactory.getWeblogger()
                .getFileContentManager();
        RollerMessages msgs = new RollerMessages();
        if (!cmgr.canSave(weblog, mediaFile.getName(),
                mediaFile.getContentType(), mediaFile.getLength(), msgs)) {
            throw new FileIOException(msgs.toString());
        }
        cmgr.saveFileContent(weblog, mediaFile.getId(), is);

        if (mediaFile.isImageFile()) {
            updateThumbnail(mediaFile);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaFile getMediaFile(String id) throws WebloggerException {
        return getMediaFile(id, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaFile getMediaFile(String id, boolean includeContent)
            throws WebloggerException {
        MediaFile mediaFile = (MediaFile) this.strategy.load(MediaFile.class,
                id);
        if (includeContent) {
            FileContentManager cmgr = WebloggerFactory.getWeblogger()
                    .getFileContentManager();

            FileContent content = cmgr.getFileContent(mediaFile.getDirectory()
                    .getWeblog(), id);
            mediaFile.setContent(content);

            try {
                FileContent thumbnail = cmgr.getFileContent(mediaFile
                        .getDirectory().getWeblog(), id + "_sm");
                mediaFile.setThumbnailContent(thumbnail);

            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Cannot load thumbnail for image " + id, e);
                } else {
                    log.warn("Cannot load thumbnail for image " + id);
                }
            }
        }
        return mediaFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaFileDirectory getMediaFileDirectoryByName(Weblog weblog,
            String name) throws WebloggerException {

        name = name.startsWith("/") ? name.substring(1) : name;

        log.debug("Looking up weblog|media file directory: " + weblog.getHandle() + "|" + name);

        TypedQuery<MediaFileDirectory> q = this.strategy
                .getNamedQuery("MediaFileDirectory.getByWeblogAndName", MediaFileDirectory.class);
        q.setParameter(1, weblog);
        q.setParameter(2, name);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaFile getMediaFileByPath(Weblog weblog, String path)
            throws WebloggerException {

        // get directory
        String fileName = path;
        MediaFileDirectory mdir;
        int slash = path.lastIndexOf('/');
        if (slash > 0) {
            mdir = getMediaFileDirectoryByName(weblog, path.substring(0, slash));
        } else {
            mdir = getDefaultMediaFileDirectory(weblog);
        }
        if (slash != -1) {
            fileName = fileName.substring(slash + 1);
        }
        return mdir.getMediaFile(fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaFile getMediaFileByOriginalPath(Weblog weblog, String origpath)
            throws WebloggerException {

        if (null == origpath) {
            return null;
        }

        if (!origpath.startsWith("/")) {
            origpath = "/" + origpath;
        }

        TypedQuery<MediaFile> q = this.strategy
                .getNamedQuery("MediaFile.getByWeblogAndOrigpath", MediaFile.class);
        q.setParameter(1, weblog);
        q.setParameter(2, origpath);
        MediaFile mf;
        try {
            mf = q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
        FileContentManager cmgr = WebloggerFactory.getWeblogger()
                .getFileContentManager();
        FileContent content = cmgr.getFileContent(
                mf.getDirectory().getWeblog(), mf.getId());
        mf.setContent(content);
        return mf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaFileDirectory getMediaFileDirectory(String id)
            throws WebloggerException {
        return (MediaFileDirectory) this.strategy.load(
                MediaFileDirectory.class, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MediaFileDirectory getDefaultMediaFileDirectory(Weblog weblog)
            throws WebloggerException {
        return getMediaFileDirectoryByName(weblog, DEFAULT_DIRECTORY_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MediaFileDirectory> getMediaFileDirectories(Weblog weblog)
            throws WebloggerException {

        TypedQuery<MediaFileDirectory> q = this.strategy.getNamedQuery("MediaFileDirectory.getByWeblog",
                MediaFileDirectory.class);
        q.setParameter(1, weblog);
        return q.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeMediaFile(Weblog weblog, MediaFile mediaFile)
            throws WebloggerException {
        FileContentManager cmgr = WebloggerFactory.getWeblogger()
                .getFileContentManager();

        this.strategy.remove(mediaFile);

        // Refresh associated parent for changes
        strategy.refresh(mediaFile.getDirectory());

        // update weblog last modified date. date updated by saveWeblog()
        roller.getWeblogManager().saveWeblog(weblog);

        try {
            cmgr.deleteFile(weblog, mediaFile.getId());
            // Now thumbnail
            cmgr.deleteFile(weblog, mediaFile.getId() + "_sm");
        } catch (Exception e) {
            log.debug("File to be deleted already unavailable in the file store");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MediaFile> fetchRecentPublicMediaFiles(int length)
            throws WebloggerException {

        String queryString = "SELECT m FROM MediaFile m WHERE m.sharedForGallery = true order by m.dateUploaded";
        TypedQuery<MediaFile> query = strategy.getDynamicQuery(queryString, MediaFile.class);
        query.setFirstResult(0);
        query.setMaxResults(length);
        return query.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public List<MediaFile> searchMediaFiles(Weblog weblog, MediaFileFilter filter) throws WebloggerException {
        StringBuilder whereClause = new StringBuilder();
        List<Object> params = new ArrayList<>();

        buildWhereClause(weblog, filter, whereClause, params);

        String queryString = "SELECT m FROM MediaFile m WHERE " + whereClause.toString() + buildOrderBy(filter);

        TypedQuery<MediaFile> query = strategy.getDynamicQuery(queryString, MediaFile.class);
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        if (filter.getOffset() != 0) {
            query.setFirstResult(filter.getOffset());
        }
        if (filter.getMaxResults() != -1) {
            query.setMaxResults(filter.getMaxResults());
        }
        return query.getResultList();
    }

    private void buildWhereClause(Weblog weblog, MediaFileFilter filter, StringBuilder whereClause,
            List<Object> params) {
        params.add(weblog);
        whereClause.append("m.directory.weblog = ?").append(params.size());

        appendNameFilter(filter.getName(), whereClause, params);
        appendSizeFilter(filter.getSize(), filter.getSizeFilterType(), whereClause, params);
        appendTagFilter(filter.getTags(), whereClause, params);
        appendTypeFilter(filter.getType(), whereClause, params);
    }

    private void appendNameFilter(String name, StringBuilder whereClause, List<Object> params) {
        if (!StringUtils.isEmpty(name)) {
            String nameFilter = name.trim();
            if (!nameFilter.endsWith("%")) {
                nameFilter = nameFilter + "%";
            }
            params.add(nameFilter);
            whereClause.append(" AND m.name like ?").append(params.size());
        }
    }

    private void appendSizeFilter(long size, MediaFileFilter.SizeFilterType type, StringBuilder whereClause,
            List<Object> params) {
        if (size > 0) {
            params.add(size);
            whereClause.append(" AND m.length ");
            switch (type) {
                case GT:
                    whereClause.append(">");
                    break;
                case GTE:
                    whereClause.append(">=");
                    break;
                case EQ:
                    whereClause.append("=");
                    break;
                case LT:
                    whereClause.append("<");
                    break;
                case LTE:
                    whereClause.append("<=");
                    break;
                default:
                    whereClause.append("=");
                    break;
            }
            whereClause.append(" ?").append(params.size());
        }
    }

    private void appendTagFilter(List<String> tags, StringBuilder whereClause, List<Object> params) {
        if (tags != null && tags.size() > 1) {
            whereClause.append(" AND EXISTS (SELECT t FROM MediaFileTag t WHERE t.mediaFile = m and t.name IN (");
            for (String tag : tags) {
                params.add(tag);
                whereClause.append("?").append(params.size()).append(",");
            }
            whereClause.deleteCharAt(whereClause.lastIndexOf(","));
            whereClause.append("))");
        } else if (tags != null && tags.size() == 1) {
            params.add(tags.get(0));
            whereClause.append(" AND EXISTS (SELECT t FROM MediaFileTag t WHERE t.mediaFile = m and t.name = ?")
                    .append(params.size()).append(")");
        }
    }

    private void appendTypeFilter(MediaFileType type, StringBuilder whereClause, List<Object> params) {
        if (type != null) {
            if (type == MediaFileType.OTHERS) {
                for (MediaFileType t : MediaFileType.values()) {
                    if (t != MediaFileType.OTHERS) {
                        params.add(t.getContentTypePrefix() + "%");
                        whereClause.append(" AND m.contentType not like ?").append(params.size());
                    }
                }
            } else {
                params.add(type.getContentTypePrefix() + "%");
                whereClause.append(" AND m.contentType like ?").append(params.size());
            }
        }
    }

    private String buildOrderBy(MediaFileFilter filter) {
        if (filter.getOrder() != null) {
            switch (filter.getOrder()) {
                case NAME:
                    return " order by m.name";
                case DATE_UPLOADED:
                    return " order by m.dateUploaded";
                case TYPE:
                    return " order by m.contentType";
                default:
            }
        }
        return " order by m.name";
    }

    /**
     * Does mediafile storage require any upgrading; checks for existence of
     * migration status file.
     */
    public boolean isFileStorageUpgradeRequired() {
        String uploadsDirName = WebloggerConfig.getProperty("uploads.dir");
        File uploadsDir = new File(uploadsDirName);
        if (uploadsDir.exists() && uploadsDir.isDirectory()) {
            Properties props = new Properties();
            try (InputStream is = new FileInputStream(uploadsDirName
                    + File.separator + MIGRATION_STATUS_FILENAME)) {
                props.load(is);

            } catch (IOException ex) {
                return true;
            }
            if (props.getProperty("complete") != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Run mediafile storage upgrade, copying files to new storage system;
     * creates migration status file only when work is complete.
     */
    /**
     * Run mediafile storage upgrade, copying files to new storage system;
     * creates migration status file only when work is complete.
     */
    public List<String> upgradeFileStorage() {
        List<String> msgs = new ArrayList<>();
        String oldDirName = WebloggerConfig.getProperty("uploads.dir");

        if (oldDirName != null) {
            try {
                migrateAllWeblogs(oldDirName);
                createMigrationStatusFile(oldDirName);
            } catch (Exception ioex) {
                log.error("ERROR upgrading", ioex);
            }
        }
        msgs.add("Migration complete!");
        return msgs;
    }

    private void migrateAllWeblogs(String oldDirName) throws WebloggerException {
        File uploadsDir = new File(oldDirName);
        File[] dirs = uploadsDir.listFiles();
        if (dirs == null) {
            return;
        }

        WeblogManager wmgr = this.roller.getWeblogManager();
        for (File dir : dirs) {
            if (dir.isDirectory()) {
                Weblog weblog = wmgr.getWeblogByHandle(dir.getName(), null);
                if (weblog != null) {
                    migrateWeblog(weblog, dir);
                }
            }
        }
    }

    private void migrateWeblog(Weblog weblog, File oldWeblogDir) throws WebloggerException {
        log.info("Migrating weblog: " + weblog.getHandle());
        User chosenUser = findAdminUser(weblog);

        MediaFileDirectory root = getDefaultMediaFileDirectory(weblog);
        if (root == null) {
            root = createDefaultMediaFileDirectory(weblog);
            roller.flush();
        }

        upgradeUploadsDir(weblog, chosenUser, oldWeblogDir, root);
    }

    private User findAdminUser(Weblog weblog) throws WebloggerException {
        WeblogManager wmgr = this.roller.getWeblogManager();
        List<User> users = wmgr.getWeblogUsers(weblog, true);
        User chosenUser = users.get(0);
        for (User user : users) {
            if (user.hasGlobalPermission("admin")) {
                return user;
            }
        }
        return chosenUser;
    }

    private void createMigrationStatusFile(String oldDirName) {
        Properties props = new Properties();
        props.setProperty("complete", "true");
        try (FileOutputStream fos = new FileOutputStream(oldDirName + File.separator + MIGRATION_STATUS_FILENAME)) {
            props.store(fos, "Migration is complete!");
        } catch (IOException e) {
            log.error("Error creating migration status file", e);
        }
    }

    private void upgradeUploadsDir(Weblog weblog, User user, File oldDir, MediaFileDirectory newDir) {
        log.debug("Upgrading dir: " + oldDir.getAbsolutePath());
        if (newDir == null) {
            log.error("newDir cannot be null");
            return;
        }

        File[] files = oldDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                upgradeNestedDir(weblog, user, file, newDir);
            } else {
                migrateFile(weblog, user, file, newDir);
            }
        }

        try {
            roller.flush();
        } catch (WebloggerException ex) {
            log.error("ERROR flushing changes to dir: " + newDir.getName(), ex);
        }
    }

    private void upgradeNestedDir(Weblog weblog, User user, File file, MediaFileDirectory parentDir) {
        MediaFileDirectory secondDir = null;
        try {
            if (weblog.hasMediaFileDirectory(file.getName())) {
                secondDir = weblog.getMediaFileDirectory(file.getName());
            } else {
                secondDir = new MediaFileDirectory(weblog, file.getName(), null);
                roller.getMediaFileManager().createMediaFileDirectory(secondDir);
                roller.flush();
            }
        } catch (WebloggerException ex) {
            log.error("ERROR creating/getting directory: " + file.getName(), ex);
        }

        if (secondDir != null) {
            upgradeUploadsDir(weblog, user, file, secondDir);
        }
    }

    private void migrateFile(Weblog weblog, User user, File file, MediaFileDirectory newDir) {
        if (newDir.hasMediaFile(file.getName())) {
            log.debug("    Skipping file that already exists: " + file.getName());
            return;
        }

        String originalPath = "/" + newDir.getName() + "/" + file.getName();
        log.debug("Upgrade file with original path: " + originalPath);

        MediaFile mf = createMediaFileMetadata(weblog, user, file, newDir, originalPath);

        try (InputStream is = new FileInputStream(file)) {
            mf.setInputStream(is);
            RollerMessages messages = new RollerMessages();
            this.roller.getMediaFileManager().createMediaFile(weblog, mf, messages);
            newDir.getMediaFiles().add(mf);
            log.info(messages.toString());
        } catch (WebloggerException | IOException ex) {
            log.error("ERROR writing/reading file: " + file.getAbsolutePath(), ex);
        }
    }

    private MediaFile createMediaFileMetadata(Weblog weblog, User user, File file, MediaFileDirectory newDir,
            String originalPath) {
        MediaFile mf = new MediaFile();
        mf.setName(file.getName());
        mf.setDescription(file.getName());
        mf.setOriginalPath(originalPath);
        mf.setDateUploaded(new Timestamp(file.lastModified()));
        mf.setLastUpdated(new Timestamp(file.lastModified()));
        mf.setDirectory(newDir);
        mf.setWeblog(weblog);
        mf.setCreatorUserName(user.getUserName());
        mf.setSharedForGallery(Boolean.FALSE);
        mf.setLength(file.length());
        mf.setContentType(Utilities.getContentTypeFromFileName(file.getName()));
        return mf;
    }

    @Override
    public void removeAllFiles(Weblog website) throws WebloggerException {
        removeMediaFileDirectory(getDefaultMediaFileDirectory(website));
    }

    @Override
    public void removeMediaFileDirectory(MediaFileDirectory dir)
            throws WebloggerException {
        if (dir == null) {
            return;
        }
        FileContentManager cmgr = WebloggerFactory.getWeblogger()
                .getFileContentManager();
        Set<MediaFile> files = dir.getMediaFiles();
        for (MediaFile mf : files) {
            try {
                cmgr.deleteFile(dir.getWeblog(), mf.getId());
                // Now thumbnail
                cmgr.deleteFile(dir.getWeblog(), mf.getId() + "_sm");
            } catch (Exception e) {
                log.debug("File to be deleted already unavailable in the file store");
            }
            this.strategy.remove(mf);
        }

        dir.getWeblog().getMediaFileDirectories().remove(dir);

        // Contained media files
        roller.flush();

        this.strategy.remove(dir);

        // Refresh associated parent
        roller.flush();
    }

    @Override
    public void removeMediaFileTag(String name, MediaFile entry)
            throws WebloggerException {

        for (Iterator<MediaFileTag> it = entry.getTags().iterator(); it.hasNext();) {
            MediaFileTag tag = it.next();
            if (tag.getName().equals(name)) {

                // Call back the entity to adjust its internal state
                entry.onRemoveTag(name);

                // Refresh it from database
                this.strategy.remove(tag);

                // Refresh it from the collection
                it.remove();
            }
        }
    }
}
