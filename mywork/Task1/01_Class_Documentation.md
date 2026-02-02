# Class Documentation - Apache Roller Subsystems

This document provides comprehensive class-level documentation for the **Weblog/Content**, **User/Role**, and **Search/Indexing** subsystems of Apache Roller, generated via recursive call-graph analysis.

---

# Part 1: Weblog and Content Subsystem

## 1.1 Interfaces

---

### WeblogEntryManager
**Path**: `org.apache.roller.weblogger.business.WeblogEntryManager`

**Primary Role**: Business interface that defines all operations for managing blog entries, comments, categories, tags, and hit counts. Acts as the primary API for the content subsystem.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `saveWeblogEntry(WeblogEntry)` | Persists a new or updated blog entry with tag aggregation and ping scheduling |
| `getWeblogEntry(String id)` | Retrieves an entry by its unique ID |
| `getWeblogEntries(WeblogEntrySearchCriteria)` | Queries entries with complex filtering (date, status, category, tags) |
| `getWeblogEntryByAnchor(Weblog, String)` | Retrieves an entry by its URL anchor with caching |
| `removeWeblogEntry(WeblogEntry)` | Deletes an entry and all associated comments/tags |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `EntryEdit`, `Entries`, `EntryRemove`, `FeedServlet`, `PageServlet`, `CommentServlet`, `WeblogEntriesListPager`, `SiteModel`, `SearchResultsModel`, `WeblogCalendarModel`, `MetaWeblogAPIHandler`, `EntryCollection` |
| **Outgoing** | `JPAPersistenceStrategy`, `WeblogManager`, `AutoPingManager` |

---

### WeblogManager
**Path**: `org.apache.roller.weblogger.business.WeblogManager`

**Primary Role**: Business interface for managing `Weblog` objects (websites). Handles creation, deletion, and retrieval of blogs.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `saveWeblog(Weblog)` | Persists weblog changes and updates lastModified timestamp |
| `getWeblogByHandle(String, Boolean)` | Retrieves a weblog by its unique handle |
| `removeWeblog(Weblog)` | Deletes a weblog and all its content |
| `getWeblogs(Boolean, Boolean, Date, Date, int, int)` | Queries weblogs with filtering |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAWeblogEntryManagerImpl`, `CreateWeblog`, `WeblogConfig`, `Maintenance`, `WeblogPermission` |
| **Outgoing** | `JPAPersistenceStrategy`, `IndexManager`, `MediaFileManager` |

---

## 1.2 Implementations

---

### JPAWeblogEntryManagerImpl
**Path**: `org.apache.roller.weblogger.business.jpa.JPAWeblogEntryManagerImpl`

**Primary Role**: JPA-based implementation of `WeblogEntryManager`. Contains all persistence logic for entries, comments, categories, and tags. Uses `JPAPersistenceStrategy` for database operations.

**Key Methods**:
| Method | Description | Fan-In |
|--------|-------------|--------|
| `getWeblogEntries(WeblogEntrySearchCriteria)` | Dynamic JPQL query builder for entry retrieval | 50+ |
| `saveWeblogEntry(WeblogEntry)` | Handles defaults, tag aggregation, and side effects | 15+ |
| `getWeblogEntry(String)` | Simple ID-based lookup via `strategy.load()` | 30+ |
| `getWeblogEntryByAnchor(Weblog, String)` | Cached lookup with `entryAnchorToIdMap` | 20+ |
| `getComments(WeblogEntry, CommentSearchCriteria)` | Retrieves comments with filtering | 10+ |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | All UI Actions (entries), Feed/Page Servlets, Pagers, Models, API Handlers |
| **Outgoing** | `JPAPersistenceStrategy.store()`, `JPAPersistenceStrategy.load()`, `JPAPersistenceStrategy.getNamedQuery()`, `WeblogManager.saveWeblog()`, `AutoPingManager.queueApplicableAutoPings()` |

---

### JPAWeblogManagerImpl
**Path**: `org.apache.roller.weblogger.business.jpa.JPAWeblogManagerImpl`

**Primary Role**: JPA-based implementation of `WeblogManager`. Manages weblog lifecycle and provides queries for weblog retrieval.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `saveWeblog(Weblog)` | Persists weblog via `strategy.store()` |
| `getWeblogByHandle(String, Boolean)` | Cached lookup with `weblogHandleToIdMap` |
| `removeWeblog(Weblog)` | Cascading delete of all weblog content |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAWeblogEntryManagerImpl`, UI Actions, `WeblogPermission.getWeblog()` |
| **Outgoing** | `JPAPersistenceStrategy`, `WeblogEntryManager`, `IndexManager`, `UserManager` |

---

## 1.3 POJOs (Data Objects)

---

### Weblog
**Path**: `org.apache.roller.weblogger.pojos.Weblog`

**Primary Role**: JPA entity representing a website/blog. Root aggregate for content. Contains configuration, theme settings, and relationships to categories and entries.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getCreator()` | Returns the `User` who created this weblog (calls `UserManager`) |
| `getTheme()` | Returns the active theme (calls `ThemeManager`) |
| `hasUserPermission(User, String)` | Checks if user has permission (calls `UserManager.checkPermission()`) |
| `getRecentWeblogEntries(int)` | Returns recent entries (calls `WeblogEntryManager`) |
| `getAbsoluteURL()` | Returns full URL (calls `URLStrategy`) |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAWeblogManagerImpl`, `JPAWeblogEntryManagerImpl`, `WeblogEntry`, `WeblogCategory`, UI Actions |
| **Outgoing** | `WebloggerFactory.getWeblogger()` → `UserManager`, `ThemeManager`, `WeblogEntryManager`, `URLStrategy` |

---

### WeblogEntry
**Path**: `org.apache.roller.weblogger.pojos.WeblogEntry`

**Primary Role**: JPA entity representing a single blog post. Contains title, content, publication status, and relationships to tags, comments, and attributes.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getCreator()` | Returns the author `User` (calls `UserManager`) |
| `getComments()` | Returns comments (calls `WeblogEntryManager.getComments()`) |
| `getPermalink()` | Returns permanent URL (calls `URLStrategy`) |
| `isPublished()` | Returns true if status is PUBLISHED |
| `createAnchorBase()` | Generates URL slug (calls `WeblogEntryManager.createAnchor()`) |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAWeblogEntryManagerImpl`, `IndexOperation`, `EntryEdit`, Pagers, Models |
| **Outgoing** | `WebloggerFactory.getWeblogger()` → `UserManager`, `WeblogEntryManager`, `URLStrategy` |

---

### WeblogEntryComment
**Path**: `org.apache.roller.weblogger.pojos.WeblogEntryComment`

**Primary Role**: JPA entity representing a comment on a blog post. Contains commenter info, content, approval status.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getWeblogEntry()` | Returns the parent entry |
| `getStatus()` | Returns approval status (APPROVED, PENDING, SPAM, DISAPPROVED) |
| `setApproved()` | Sets status to APPROVED |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAWeblogEntryManagerImpl`, `Comments` (UI), `CommentServlet`, `IndexOperation` |
| **Outgoing** | `WeblogEntry` |

---

### WeblogCategory
**Path**: `org.apache.roller.weblogger.pojos.WeblogCategory`

**Primary Role**: JPA entity representing a category for organizing blog entries.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `retrieveWeblogEntries(boolean, boolean)` | Gets entries in category (calls `WeblogEntryManager`) |
| `isInUse()` | Checks if category has entries (calls `WeblogEntryManager`) |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAWeblogEntryManagerImpl`, `Weblog`, `CategoryEdit`, `CategoryRemove` |
| **Outgoing** | `WebloggerFactory.getWeblogger()` → `WeblogEntryManager` |

---

### WeblogEntryTag
**Path**: `org.apache.roller.weblogger.pojos.WeblogEntryTag`

**Primary Role**: JPA entity representing a tag applied to an entry.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getName()` | Returns the tag name |
| `getCreator()` | Returns the user who added the tag (calls `UserManager`) |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `WeblogEntry`, `JPAWeblogEntryManagerImpl` |
| **Outgoing** | `UserManager.getUserByUserName()` |

---

### WeblogEntryTagAggregate
**Path**: `org.apache.roller.weblogger.pojos.WeblogEntryTagAggregate`

**Primary Role**: JPA entity storing aggregate tag counts per weblog for tag cloud generation.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getName()` | Tag name |
| `getTotal()` | Total count of entries with this tag |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAWeblogEntryManagerImpl.updateTagCount()` |
| **Outgoing** | None (pure data) |

---

### WeblogEntrySearchCriteria
**Path**: `org.apache.roller.weblogger.pojos.WeblogEntrySearchCriteria`

**Primary Role**: Value object (not JPA entity) used to specify search parameters for querying entries.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `setWeblog(Weblog)` | Filter by weblog |
| `setStatus(PubStatus)` | Filter by publication status |
| `setTags(List<String>)` | Filter by tags |
| `setCatName(String)` | Filter by category |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | UI Actions, Pagers, Models, API Handlers |
| **Outgoing** | `JPAWeblogEntryManagerImpl.getWeblogEntries()` |

---

# Part 2: User and Role Management Subsystem

## 2.1 Interfaces

---

### UserManager
**Path**: `org.apache.roller.weblogger.business.UserManager`

**Primary Role**: Business interface defining all user, role, and permission management operations. Central authorization API.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getUserByUserName(String)` | Retrieves user by username (cached) |
| `checkPermission(RollerPermission, User)` | Authorization check - single entry point |
| `getWeblogPermission(Weblog, User)` | Gets user's permission on a specific weblog |
| `getRoles(User)` | Gets all global roles for a user |
| `addUser(User)` | Creates a new user with default role |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `RollerUserDetailsService`, `UISecurityInterceptor`, `UIAction`, `Weblog`, `WeblogEntry`, `MediaFile`, `GlobalPermission`, `WeblogPermission`, all API handlers |
| **Outgoing** | `JPAPersistenceStrategy` |

---

## 2.2 Implementations

---

### JPAUserManagerImpl
**Path**: `org.apache.roller.weblogger.business.jpa.JPAUserManagerImpl`

**Primary Role**: JPA-based implementation of `UserManager`. Implements caching (`userNameToIdMap`) for user lookups and handles all permission logic.

**Key Methods**:
| Method | Description | Fan-In |
|--------|-------------|--------|
| `getUserByUserName(String)` | Cached lookup via `userNameToIdMap` | 40+ |
| `checkPermission(RollerPermission, User)` | Two-phase check: WeblogPermission then GlobalPermission | 25+ |
| `getWeblogPermission(Weblog, User)` | Named query lookup | 15+ |
| `getRoles(User)` | Named query for `UserRole` entities | 10+ |
| `addUser(User)` | Stores user and grants "editor" role | 5+ |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | Spring Security, UI interceptors, all POJOs with `getCreator()`, all permission checks |
| **Outgoing** | `JPAPersistenceStrategy.store()`, `JPAPersistenceStrategy.load()`, `JPAPersistenceStrategy.getNamedQuery()` |

---

## 2.3 POJOs (Data Objects)

---

### User
**Path**: `org.apache.roller.weblogger.pojos.User`

**Primary Role**: JPA entity representing a user account. Contains authentication data and profile information.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getUserName()` | Returns unique username |
| `hasGlobalPermission(String)` | Checks global permission (calls `UserManager.checkPermission()`) |
| `hasGlobalPermissions(List<String>)` | Checks multiple permissions |
| `resetPassword(String, String)` | Resets password using Spring Security encoder |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAUserManagerImpl`, `RollerUserDetailsService`, `Weblog`, `WeblogEntry`, `MediaFile` |
| **Outgoing** | `WebloggerFactory.getWeblogger()` → `UserManager.checkPermission()` |

---

### UserRole
**Path**: `org.apache.roller.weblogger.pojos.UserRole`

**Primary Role**: JPA entity linking a username to a global role (e.g., "admin", "editor").

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getUserName()` | Returns the associated username |
| `getRole()` | Returns the role name |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAUserManagerImpl.getRoles()`, `GlobalPermission` |
| **Outgoing** | None (pure data) |

---

### WeblogPermission
**Path**: `org.apache.roller.weblogger.pojos.WeblogPermission`

**Primary Role**: JPA entity representing a user's permission on a specific weblog. Extends `ObjectPermission`.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `implies(RollerPermission)` | Checks if this permission implies the requested one |
| `getWeblog()` | Returns the associated weblog (calls `WeblogManager`) |
| `getUser()` | Returns the associated user (calls `UserManager`) |
| `getActionsAsList()` | Returns list of actions (EDIT_DRAFT, POST, ADMIN) |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAUserManagerImpl.checkPermission()`, `JPAUserManagerImpl.getWeblogPermission()` |
| **Outgoing** | `WebloggerFactory.getWeblogger()` → `WeblogManager`, `UserManager` |

---

### GlobalPermission
**Path**: `org.apache.roller.weblogger.pojos.GlobalPermission`

**Primary Role**: Non-persistent object representing system-wide permissions derived from `UserRole`. Created on-the-fly during authorization.

**Key Methods**:
| Method | Description |
|--------|-------------|
| Constructor `GlobalPermission(User)` | Fetches user's roles and populates actions |
| `implies(RollerPermission)` | Returns true if user has "admin" role |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `JPAUserManagerImpl.checkPermission()` |
| **Outgoing** | `WebloggerFactory.getWeblogger()` → `UserManager.getRoles(user)` |

---

### ObjectPermission
**Path**: `org.apache.roller.weblogger.pojos.ObjectPermission`

**Primary Role**: Abstract base class for object-specific permissions (like `WeblogPermission`).

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getObjectId()` | Returns the ID of the protected object |
| `getObjectType()` | Returns the type of protected object |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `WeblogPermission` extends this |
| **Outgoing** | None |

---

### RollerPermission
**Path**: `org.apache.roller.weblogger.pojos.RollerPermission`

**Primary Role**: Abstract base class for all permission types in Roller.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `implies(RollerPermission)` | Abstract method for permission checking |
| `getActionsAsList()` | Returns list of action strings |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `GlobalPermission`, `ObjectPermission` extend this |
| **Outgoing** | None |

---

# Part 3: Search and Indexing Subsystem

## 3.1 Interfaces

---

### IndexManager
**Path**: `org.apache.roller.weblogger.business.search.IndexManager`

**Primary Role**: Business interface defining search and indexing operations. Abstracts the underlying search technology (Lucene).

**Key Methods**:
| Method | Description |
|--------|-------------|
| `search(...)` | Executes a search query and returns results |
| `addEntryReIndexOperation(WeblogEntry)` | Schedules an entry for (re)indexing |
| `removeEntryIndexOperation(WeblogEntry)` | Removes an entry from the index |
| `rebuildWebsiteIndex(Weblog)` | Rebuilds entire index for a weblog |
| `initialize()` | Initializes the index on startup |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `EntryEdit`, `EntryRemove`, `Comments`, `CommentServlet`, `Maintenance`, `SearchResultsModel`, `EntryCollection` |
| **Outgoing** | `ThreadManager`, `WeblogEntryManager` |

---

## 3.2 Implementations

---

### LuceneIndexManager
**Path**: `org.apache.roller.weblogger.business.search.lucene.LuceneIndexManager`

**Primary Role**: Lucene-based implementation of `IndexManager`. Manages index directory, shared reader, and operation scheduling.

**Key Methods**:
| Method | Description | Fan-In |
|--------|-------------|--------|
| `search(...)` | Creates `SearchOperation`, executes foreground, converts hits | 10+ |
| `addEntryReIndexOperation(WeblogEntry)` | Creates `ReIndexEntryOperation`, schedules background | 8+ |
| `removeEntryIndexOperation(WeblogEntry)` | Creates `RemoveEntryOperation`, schedules background | 5+ |
| `scheduleIndexOperation(IndexOperation)` | Submits to `ThreadManager.executeInBackground()` | 10+ |
| `getSharedIndexReader()` | Returns cached `DirectoryReader` | 5+ |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | UI Actions, Servlets, Models, API Handlers |
| **Outgoing** | `ThreadManager`, `WeblogEntryManager`, Lucene API (`IndexWriter`, `DirectoryReader`, `IndexSearcher`) |

---

## 3.3 Index Operations (Command Pattern)

---

### IndexOperation
**Path**: `org.apache.roller.weblogger.business.search.lucene.IndexOperation`

**Primary Role**: Abstract base class for all index operations. Implements `Runnable`. Provides shared utilities like `getDocument(WeblogEntry)`.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `run()` | Template method that calls `doRun()` |
| `doRun()` | Abstract method - implemented by subclasses |
| `getDocument(WeblogEntry)` | Converts entry to Lucene `Document` |
| `beginWriting()` | Opens `IndexWriter` |
| `endWriting()` | Closes `IndexWriter` |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `LuceneIndexManager` creates and schedules operations |
| **Outgoing** | `WeblogEntryManager.getWeblogEntry()`, `WeblogEntry.getComments()`, Lucene `IndexWriter` |

---

### WriteToIndexOperation
**Path**: `org.apache.roller.weblogger.business.search.lucene.WriteToIndexOperation`

**Primary Role**: Abstract base for write operations. Handles write lock acquisition and shared reader invalidation.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `run()` | Acquires write lock, calls `doRun()`, releases lock, resets reader |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `AddEntryOperation`, `ReIndexEntryOperation`, `RemoveEntryOperation`, etc. extend this |
| **Outgoing** | `LuceneIndexManager.getReadWriteLock()`, `LuceneIndexManager.resetSharedReader()` |

---

### ReadFromIndexOperation
**Path**: `org.apache.roller.weblogger.business.search.lucene.ReadFromIndexOperation`

**Primary Role**: Abstract base for read operations. Does not acquire write lock.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `run()` | Calls `doRun()` without locking |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `SearchOperation` extends this |
| **Outgoing** | None |

---

### AddEntryOperation
**Path**: `org.apache.roller.weblogger.business.search.lucene.AddEntryOperation`

**Primary Role**: Adds a new entry to the Lucene index.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `doRun()` | Re-fetches entry, creates document, adds to index |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `LuceneIndexManager.addEntryIndexOperation()` |
| **Outgoing** | `WeblogEntryManager.getWeblogEntry()`, `IndexWriter.addDocument()` |

---

### ReIndexEntryOperation
**Path**: `org.apache.roller.weblogger.business.search.lucene.ReIndexEntryOperation`

**Primary Role**: Updates an existing entry in the index (delete + add).

**Key Methods**:
| Method | Description |
|--------|-------------|
| `doRun()` | Deletes old document by ID, adds new document |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `LuceneIndexManager.addEntryReIndexOperation()` |
| **Outgoing** | `WeblogEntryManager.getWeblogEntry()`, `IndexWriter.deleteDocuments()`, `IndexWriter.addDocument()` |

---

### RemoveEntryOperation
**Path**: `org.apache.roller.weblogger.business.search.lucene.RemoveEntryOperation`

**Primary Role**: Removes an entry from the Lucene index.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `doRun()` | Deletes document by entry ID |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `LuceneIndexManager.removeEntryIndexOperation()` |
| **Outgoing** | `IndexWriter.deleteDocuments()` |

---

### RebuildWebsiteIndexOperation
**Path**: `org.apache.roller.weblogger.business.search.lucene.RebuildWebsiteIndexOperation`

**Primary Role**: Rebuilds the entire index for a specific weblog.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `doRun()` | Deletes all docs for weblog, re-indexes all published entries |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `LuceneIndexManager.rebuildWebsiteIndex()` |
| **Outgoing** | `WeblogEntryManager.getWeblogEntries()`, `IndexWriter` |

---

### RemoveWebsiteIndexOperation
**Path**: `org.apache.roller.weblogger.business.search.lucene.RemoveWebsiteIndexOperation`

**Primary Role**: Removes all indexed entries for a weblog (used when weblog is deleted).

**Key Methods**:
| Method | Description |
|--------|-------------|
| `doRun()` | Deletes all documents with matching weblog handle |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `LuceneIndexManager.removeWebsiteIndex()` |
| **Outgoing** | `IndexWriter.deleteDocuments()` |

---

### SearchOperation
**Path**: `org.apache.roller.weblogger.business.search.lucene.SearchOperation`

**Primary Role**: Executes a search query against the Lucene index.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `doRun()` | Parses query, executes search, stores results |
| `setTerm(String)` | Sets the search term |
| `setWeblogHandle(String)` | Filters by weblog |
| `getResults()` | Returns `TopFieldDocs` |
| `getSearcher()` | Returns `IndexSearcher` for document retrieval |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `LuceneIndexManager.search()` |
| **Outgoing** | `LuceneIndexManager.getSharedIndexReader()`, Lucene `IndexSearcher`, `MultiFieldQueryParser` |

---

## 3.4 Utility Classes

---

### FieldConstants
**Path**: `org.apache.roller.weblogger.business.search.lucene.FieldConstants`

**Primary Role**: Defines constants for Lucene document field names.

**Key Fields**:
| Field | Description |
|-------|-------------|
| `ID` | Entry ID |
| `TITLE` | Entry title |
| `CONTENT` | Entry content |
| `CATEGORY` | Category name |
| `WEBSITE_HANDLE` | Weblog handle |
| `PUBLISHED` | Publication date |
| `C_CONTENT` | Comment content |

---

### IndexUtil
**Path**: `org.apache.roller.weblogger.business.search.lucene.IndexUtil`

**Primary Role**: Utility methods for creating Lucene `Term` objects.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getTerm(String field, String value)` | Creates a Term object, handling nulls |

---

### SearchResultList
**Path**: `org.apache.roller.weblogger.business.search.SearchResultList`

**Primary Role**: Container for search results with pagination info.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getResults()` | Returns list of `WeblogEntryWrapper` |
| `getOffset()` | Returns current page offset |
| `getLimit()` | Returns results per page |

---

# Part 4: Shared Infrastructure

## 4.1 Persistence

---

### JPAPersistenceStrategy
**Path**: `org.apache.roller.weblogger.business.jpa.JPAPersistenceStrategy`

**Primary Role**: Central abstraction for all JPA operations. Manages `EntityManager` lifecycle, transactions, and provides query APIs.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `store(Object)` | Persists or merges an entity |
| `load(Class, String)` | Retrieves entity by ID |
| `remove(Object)` | Deletes an entity |
| `getNamedQuery(String, Class)` | Creates a typed named query |
| `flush()` | Commits transaction |
| `release()` | Rolls back and releases EntityManager |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | All JPA*Impl classes |
| **Outgoing** | JPA `EntityManager`, `EntityManagerFactory` |

---

### Weblogger
**Path**: `org.apache.roller.weblogger.business.Weblogger`

**Primary Role**: Main facade interface for the business tier. Provides access to all managers.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getUserManager()` | Returns `UserManager` |
| `getWeblogManager()` | Returns `WeblogManager` |
| `getWeblogEntryManager()` | Returns `WeblogEntryManager` |
| `getIndexManager()` | Returns `IndexManager` |
| `getUrlStrategy()` | Returns `URLStrategy` |
| `flush()` | Flushes all pending changes |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | `WebloggerFactory.getWeblogger()` from all POJOs and UI classes |
| **Outgoing** | All manager implementations |

---

### WebloggerFactory
**Path**: `org.apache.roller.weblogger.business.WebloggerFactory`

**Primary Role**: Singleton factory for obtaining the `Weblogger` instance.

**Key Methods**:
| Method | Description |
|--------|-------------|
| `getWeblogger()` | Returns the singleton `Weblogger` instance |
| `bootstrap()` | Initializes the Weblogger on startup |

**Interactions**:
| Direction | Classes |
|-----------|---------|
| **Incoming** | All POJOs, UI Actions, Servlets, Models |
| **Outgoing** | `Weblogger` implementation |

---

# Appendix: Cross-Subsystem Dependency Matrix

| Source Subsystem | Target Subsystem | Connection Point |
|------------------|------------------|------------------|
| Weblog | User | `Weblog.getCreator()`, `WeblogEntry.getCreator()`, `hasUserPermission()` |
| Weblog | Search | `EntryEdit` → `IndexManager.addEntryReIndexOperation()` |
| User | Weblog | `WeblogPermission.getWeblog()` |
| User | Search | None (indirect via entry creator in indexed document) |
| Search | Weblog | `IndexOperation.getDocument()` → `WeblogEntry`, `WeblogEntryManager` |
| Search | User | `IndexOperation.getDocument()` → `WeblogEntry.getCreator()` |
