# Quick Reference Card

## Project Setup Checklist

- [x] Color palette defined
- [x] Dependencies added (Navigation, Material Components, Fragment)
- [x] Authentication flow created (Entry → Login → Register)
- [x] Main app flow created (Home, Search, Favorites, Cart, Profile)
- [x] Bottom navigation bar implemented
- [x] User session management implemented
- [x] Logout functionality added

## File Organization

```
app/src/main/
├── java/com/example/temp/
│   ├── MainActivity.java (Main activity with navigation setup)
│   ├── ui/
│   │   ├── auth/ (3 fragments)
│   │   └── main/ (5 fragments)
│   └── utils/
│       └── AuthHelper.java (Session & validation)
└── res/
    ├── layout/ (8 fragment layouts + 1 activity layout)
    ├── navigation/ (2 nav graphs)
    ├── menu/ (Bottom nav menu)
    ├── drawable/ (Icon tint selector)
    └── values/
        ├── colors.xml (9 colors)
        ├── strings.xml (30+ strings)
        └── themes.xml (Styles)
```

## Color Codes Reference

| Name            | Hex Code | Usage                 |
| --------------- | -------- | --------------------- |
| Background      | #F7F7F4  | Screen backgrounds    |
| Card Background | #F2F1ED  | Input fields, cards   |
| Overlay         | #EBEAE5  | Hints, secondary text |
| Dull Black      | #222222  | Primary text          |
| Accent          | #6C5CE7  | Buttons, highlights   |
| Error           | #E74C3C  | Error states          |
| Success         | #27AE60  | Success states        |

## Navigation Routes

### Authentication Flow (Entry Point)

```
Entry → Sign In → Login
         ↓         ↓ New? → Register
         Skip      ↓
           └─ Home (Main App) ← Registration
```

### Main App Flow

```
Home (default)
├─ Search
├─ Favorites
├─ Cart
└─ Profile → Logout → Entry
```

## Key Classes & Methods

### EntryFragment

- Displays welcome message
- Two buttons: Sign In, Continue Without Signing In

### LoginFragment

- Email input with validation
- Password input (min 6 chars)
- Link to registration
- Stores user session

### RegisterFragment

- Name, Email, Password, Confirm Password
- Password confirmation validation
- Auto-login after registration

### HomeFragment, SearchFragment, FavoritesFragment, CartFragment

- Basic placeholder screens
- Ready for custom UI implementation

### ProfileFragment

- Shows user name & email (if logged in)
- Logout button (if logged in)
- Guest view (if not logged in)

### AuthHelper

- `setLoggedIn(boolean, email, name)` - Save session
- `isLoggedIn()` - Check login status
- `getUserEmail()` / `getUserName()` - Get stored data
- `logout()` - Clear session
- `isValidEmail(email)` - Email validation
- `isValidPassword(password)` - Password validation

## How to Build & Run

### 1. Build Project

```bash
./gradlew build
```

### 2. Run on Device/Emulator

```bash
./gradlew installDebug
```

### 3. Test Login Flow

- **Entry Screen**: See "Welcome" message with two buttons
- **Skip**: Go directly to Home
- **Sign In**: Enter any email + password (6+ chars)
- **Register**: Fill form and create account
- **Profile Tab**: See user info (if logged in)
- **Logout**: Returns to Entry screen

## Testing Credentials

Any valid email + 6+ char password works (simulated):

- `test@example.com` / `password123`
- `user@test.com` / `mypass123`

## Common Customizations

### Change App Colors

Edit: `res/values/colors.xml`

### Change App Name

Edit: `res/values/strings.xml` → `app_name`

### Change Welcome Text

Edit: `res/layout/fragment_entry.xml` → Text values

### Add More Tabs

1. Create Fragment class: `app/src/main/java/com/example/temp/ui/main/NewTabFragment.java`
2. Create layout: `res/layout/fragment_new_tab.xml`
3. Add to nav graph: `res/navigation/main_nav_graph.xml`
4. Add menu item: `res/menu/bottom_nav_menu.xml`

### Change Bottom Nav Icons

Edit: `res/menu/bottom_nav_menu.xml` → `android:icon` attributes
(Use custom drawable files in `res/drawable/`)

## Troubleshooting

| Issue                  | Solution                                        |
| ---------------------- | ----------------------------------------------- |
| Build errors           | Run `./gradlew clean build`                     |
| Fragment not showing   | Check navigation graph IDs match                |
| Bottom nav not working | Ensure menu item IDs match fragment IDs         |
| Colors not applying    | Clear build folder: `./gradlew clean`           |
| Imports not found      | Sync Gradle: `./gradlew --refresh-dependencies` |

## Next Steps

1. **Add Backend**: See `BACKEND_INTEGRATION.md`
2. **Customize UI**: Add images, animations, custom layouts
3. **Add Features**:
   - Search functionality
   - Product catalog
   - Shopping cart logic
   - User profile editing
4. **Testing**: Add unit tests and UI tests
5. **Deployment**: Set up release build and signing

## Architecture Overview

```
MainActivity (Single Activity Architecture)
  └─ NavHostFragment (Navigation Container)
      ├─ auth_nav_graph.xml
      │   ├─ EntryFragment
      │   ├─ LoginFragment
      │   └─ RegisterFragment
      └─ main_nav_graph.xml (included)
          ├─ HomeFragment
          ├─ SearchFragment
          ├─ FavoritesFragment
          ├─ CartFragment
          └─ ProfileFragment

AuthHelper (Shared Preferences)
  ├─ Session Management
  ├─ Input Validation
  └─ Data Storage
```

## Resources

- Android Navigation: https://developer.android.com/guide/navigation
- Material Design 3: https://m3.material.io
- Android Fragments: https://developer.android.com/guide/fragments
- Navigation Best Practices: https://developer.android.com/guide/navigation/design-navigation

## Support

For issues or questions:

1. Check README.md for detailed documentation
2. Review BACKEND_INTEGRATION.md for API setup
3. Check Android official documentation
4. Review Material Design guidelines

---

**Last Updated**: November 4, 2025
