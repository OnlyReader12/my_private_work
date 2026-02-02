# 2nd Subsystem: User and Role Management Subsystem

This subsystem provides the identity, authentication, and authorization backbone for Apache Roller, ensuring secure multi-user access.

## Important Classes and Interfaces

### Identity and Security Entities
1.  **User**: Represents a registered system user, including credentials, email, and account status.
2.  **WeblogPermission**: A granular record mapping a user to a weblog with specific rights (admin, post, edit).
3.  **GlobalPermission**: Defines system-wide administrative rights for an individual user.
4.  **UserRole**: (Legacy) Represents traditional role-based groups, now secondary to the permission model.
5.  **RollerPermission**: Extends `java.security.Permission` to implement Roller-specific security logic.
6.  **ObjectPermission**: A base class for permissions related to specific system objects.
7.  **OAuthAccessorRecord**: Stores metadata for OAuth-based API access sessions.
8.  **OAuthConsumerRecord**: Represents an authorized external application consuming Roller APIs.
9.  **RuntimeConfigProperty**: Stores site-wide configuration settings that affect user behavior and security.

### Core Business Logic
10. **UserManager**: The central interface for all user management, authentication, and authorization tasks.
11. **OAuthManager**: Manages the registry and lifecycle of OAuth consumers and accessors.
12. **JPAUserManagerImpl**: The standard implementation using JPA for persisting the security model.
13. **JPAOAuthManagerImpl**: Persistence implementation for OAuth-related metadata.
14. **CustomUserRegistry**: Interface for integrating with external identity providers like LDAP or OpenID.
15. **RollerSession**: Manages the security state and session data for the currently logged-in user.

### UI and Security Actions (Controllers)
16. **BootstrapFilter**: A security filter that ensures the system is properly initialized before access.
17. **LoginAction**: Handles the user authentication process and session initiation.
18. **RegisterAction**: Manages the self-registration process for new users.
19. **CreateUserAction**: Admin-level action for manually creating new user accounts.
20. **EditUserAction**: Allows users and admins to modify existing profile and security data.
21. **UserAdmin**: Orchestrates the administrative interface for managing the global user list.
22. **ProfileAction**: Manages the individual user profile settings and preferences.
23. **UserMembershipAction**: Handles the UI for managing user-weblog associations and permissions.

### Utilities and Support
24. **UserSearchCriteria**: A data transfer object used to filter and search for users across the system.
25. **UsersPager**: Provides pagination logic for displaying user lists in the administrative UI.
26. **RollerRequest**: A wrapper around the standard HTTP request that provides Roller-specific context (e.g., current user).
27. **SaltSource**: Provides the cryptographic salt used in secure password hashing.
28. **PasswordEncoder**: Defines the contract for securely hashing and verifying user passwords.
29. **UserValidator**: Contains business rules for validating user profiles (e.g., email format, username length).
30. **UserComparator**: Utility for sorting users by various attributes in the UI.
31. **SecurityConfig**: Manages the configuration of the security framework (e.g., Spring Security integration).
32. **LDAPUserRegistry**: A concrete implementation for syncing users with an LDAP directory.

## Subsystem Role
The User Subsystem is a cross-cutting concern. Every other subsystem calls the `UserManager` to verify authority before performing sensitive operations, making it the most critical dependency in the architecture.
