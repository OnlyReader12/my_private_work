# 1st Subsystem: Weblog and Content Subsystem

This subsystem manages the core blogging lifecycle, including content creation, categorization, commentary, media management, and the rendering of blog pages.

## Important Classes and Interfaces

### Domain Entities (POJOs)
1.  **Weblog**: The fundamental container for a blog site, storing handle, name, description, and theme settings.
2.  **WeblogEntry**: Represents a blog post, containing title, summary, text, status, and publication metadata.
3.  **WeblogCategory**: Provides a hierarchical structure for organizing entries within a weblog.
4.  **WeblogEntryComment**: Stores visitor and user comments on blog entries, including approval status.
5.  **WeblogEntryAttribute**: Key-value pairs allowing for extensible metadata on blog entries.
6.  **WeblogEntryTag**: Individual labels used for folksonomy-based organization of content.
7.  **WeblogEntryTagAggregate**: Aggregates tag frequency for generating tag clouds.
8.  **WeblogHitCount**: Tracks visitor traffic for specific weblogs.
9.  **WeblogBookmark**: Represents an external link in a user's blogroll.
10. **WeblogBookmarkFolder**: Organizes bookmarks into manageable groups.
11. **MediaFile**: Encapsulates metadata for uploaded files like images, videos, and documents.
12. **MediaFileDirectory**: Organizes media files into a virtual folder structure.
13. **WeblogTemplate**: Represents a customized layout or component for a blog's theme.
14. **Theme**: A collection of templates and resources that define the visual look of a blog.
15. **ThemeTemplate**: An interface representing a template within a shared or custom theme.
16. **CustomTemplateRendition**: Holds the actual markup and code (e.g., Velocity) for a specific template.
17. **PingTarget**: Represents an external service to be notified when the weblog is updated.
18. **PingQueueEntry**: A record of a pending ping notification to an external service.
19. **AutoPing**: Configuration for automatically triggering pings upon entry publication.

### Business Logic and Managers
20. **WeblogManager**: Orchestrates the management of weblogs, templates, and theme assignments.
21. **WeblogEntryManager**: Manages the persistence and complex querying of entries, categories, and tags.
22. **BookmarkManager**: Handles CRUD operations for bookmarks and bookmark folders.
23. **MediaFileManager**: Manages file metadata and the logical directory structure for media.
24. **FileContentManager**: Responsible for the direct byte-level storage and retrieval of file content.
25. **PingTargetManager**: Manages the registry of external ping targets.
26. **PingQueueManager**: Manages the queue of outgoing update notifications.
27. **URLStrategy**: An interface defining how the system generates URLs for various blog resources.
28. **AbstractURLStrategy**: A base implementation providing common URL generation logic.

### Rendering and UI Logic
29. **RendererManager**: A central registry that selects the correct template engine (e.g., Velocity) for rendering.
30. **RendererFactory**: An interface for creating specific `Renderer` instances.
31. **VelocityRenderer**: A concrete implementation that renders blog pages using the Velocity template engine.
32. **WeblogRequestMapper**: Parses incoming HTTP requests to resolve the target blog and resource.
33. **PageModel**: Provides the data context for rendering blog pages.
34. **FeedModel**: Specialized model for generating RSS and Atom syndication feeds.
35. **WeblogEntriesPager**: Facilitates paginated navigation through lists of blog entries in the UI.

## Subsystem Interactions
This subsystem acts as the primary data producer. It relies on the **User Subsystem** for security and updates the **Search Subsystem** whenever entries are saved or deleted.
