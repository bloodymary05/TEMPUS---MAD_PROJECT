# Android App Architecture

## Overview

This Android application implements a complete authentication flow with a bottom navigation bar containing 5 tabs. The app uses modern Android architecture with Navigation Components and Fragment-based UI.

## Design Colors

- **Background**: #F7F7F4 (Soft Beige)
- **Card Background**: #F2F1ED (Light Gray)
- **Overlay**: #EBEAE5 (Light Gray-Brown)
- **Dull Black (Text)**: #222222
- **Accent**: #6C5CE7 (Purple)
- **Error**: #E74C3C (Red)
- **Success**: #27AE60 (Green)

## App Flow

### 1. Authentication Flow (auth_nav_graph.xml)

#### Entry Fragment

- **Purpose**: Initial entry point when user is not logged in
- **Options**:
  - Sign In → Navigate to Login
  - Continue Without Signing In → Skip to Main App

#### Login Fragment

- **Purpose**: User authentication
- **Features**:
  - Email validation
  - Password validation (minimum 6 characters)
  - Link to Registration page for new users
  - Simulated login (stores credentials in SharedPreferences)

#### Registration Fragment

- **Purpose**: New user account creation
- **Features**:
  - Name, Email, Password, Confirm Password fields
  - Password validation
  - Password confirmation matching
  - Link back to Login for existing users
  - Simulated registration

### 2. Main App Flow (main_nav_graph.xml)

After successful login or skipping authentication, users access the main app with 5 tabs:

#### Tab 1: Home Fragment

- Default landing page after authentication
- Shows welcome message

#### Tab 2: Search Fragment

- Search functionality page
- Currently a placeholder for custom search UI

#### Tab 3: Favorites Fragment

- User's favorite items/content
- Placeholder for favorites display

#### Tab 4: Cart Fragment

- Shopping cart or basket
- Placeholder for cart items

#### Tab 5: Profile Fragment

- **If logged in**: Shows username, email, and Logout button
- **If guest**: Shows profile info placeholder
- Logout button triggers navigation back to Entry screen

## Project Structure

```
com.example.temp/
├── ui/
│   ├── auth/
│   │   ├── EntryFragment.java
│   │   ├── LoginFragment.java
│   │   └── RegisterFragment.java
│   └── main/
│       ├── HomeFragment.java
│       ├── SearchFragment.java
│       ├── FavoritesFragment.java
│       ├── CartFragment.java
│       └── ProfileFragment.java
├── utils/
│   └── AuthHelper.java          # Shared Preferences & validation
├── MainActivity.java             # Entry activity with navigation setup
└── resources/
    ├── layout/
    │   ├── activity_main.xml    # Main activity layout with bottom nav
    │   ├── fragment_entry.xml
    │   ├── fragment_login.xml
    │   ├── fragment_register.xml
    │   ├── fragment_home.xml
    │   ├── fragment_search.xml
    │   ├── fragment_favorites.xml
    │   ├── fragment_cart.xml
    │   └── fragment_profile.xml
    ├── navigation/
    │   ├── auth_nav_graph.xml   # Authentication flow
    │   └── main_nav_graph.xml   # Main app flow
    ├── menu/
    │   └── bottom_nav_menu.xml  # Bottom navigation menu
    ├── drawable/
    │   └── icon_tint.xml        # Color state for nav icons
    └── values/
        ├── colors.xml           # Color palette
        ├── strings.xml          # All text resources
        └── themes.xml           # App themes & styles
```

## Key Components

### AuthHelper.java

**Location**: `com/example/temp/utils/`

Utility class for authentication operations:

- `setLoggedIn()`: Save login state and user info
- `isLoggedIn()`: Check if user is logged in
- `getUserEmail()` / `getUserName()`: Retrieve stored user data
- `logout()`: Clear stored data
- `isValidEmail()`: Email validation using Android patterns
- `isValidPassword()`: Password validation (min 6 chars)

**Storage**: SharedPreferences with key `app_preferences`

### Navigation Structure

**Auth Navigation Graph** (`auth_nav_graph.xml`):

- Start: Entry Fragment
- Destinations: Login, Register
- Transitions to Main Nav Graph on successful auth

**Main Navigation Graph** (`main_nav_graph.xml`):

- Start: Home Fragment
- Destinations: Search, Favorites, Cart, Profile
- Profile has action to return to Entry on logout

## Dependencies Added

- `androidx.navigation:navigation-fragment` - Fragment navigation
- `androidx.navigation:navigation-ui` - UI integration for navigation
- `androidx.fragment:fragment` - Fragment support

## How to Customize

### Adding New Screens

1. Create a new Fragment class extending `androidx.fragment.app.Fragment`
2. Create corresponding layout XML
3. Add to appropriate navigation graph
4. If main app tab, add menu item to `bottom_nav_menu.xml`

### Modifying Colors

Edit `res/values/colors.xml` and update theme colors in `res/values/themes.xml`

### Adding Backend Integration

The `AuthHelper` class can be extended to:

- Call API endpoints instead of using SharedPreferences
- Implement real authentication logic
- Add token management for API requests

### Changing Bottom Navigation Icons

Replace icons in `bottom_nav_menu.xml` with your custom drawable resources

## User Flow Diagram

```
App Start
  ↓
Check Login Status (AuthHelper)
  ├─ NOT Logged In → Entry Fragment
  │   ├─ Sign In → Login Fragment
  │   │   ├─ New User? → Register Fragment → Home Fragment
  │   │   └─ Existing User → Home Fragment
  │   └─ Skip → Home Fragment (Guest)
  └─ Logged In → Home Fragment
       ↓
    Bottom Navigation (5 Tabs)
    ├─ Home
    ├─ Search
    ├─ Favorites
    ├─ Cart
    └─ Profile
         ├─ View User Info
         └─ Logout → Entry Fragment
```

## Testing Credentials

For testing purposes, the login validation is simulated. Any email and password (minimum 6 characters) combination will authenticate:

- Example: `test@example.com` / `password123`

## Material Design Integration

The app uses Material 3 design system:

- Material 3 theme in `themes.xml`
- Material Components (TextInputLayout, Button, BottomNavigationView)
- Material Icons (via Android drawable resources)

## Future Enhancements

- [ ] Real backend API integration
- [ ] Social login options (Google, Facebook)
- [ ] Biometric authentication
- [ ] Password reset functionality
- [ ] Email verification
- [ ] User profile editing
- [ ] Dark mode support
- [ ] Push notifications
