# Droneside

The [FPVTrackside](https://fpvtrackside.com) Companion App - A NativeScript Vue mobile application for FPV drone racing enthusiasts.

## Overview

Droneside is a cross-platform mobile application that provides comprehensive access to FPV drone racing data from the [FPVTrackside](https://fpvtrackside.com) platform. Track races, view pilot performance, analyze lap times, and stay connected with the drone racing community.

## Features

### 🏁 Race Tracking
- **Race Results**: Complete race results and timing data
- **Detailed Race Views**: Complete pilot performance with lap-by-lap breakdown
- **Podium Display**: Celebrate race winners with medal-colored position badges
- **Race Timing**: Start/end times, race duration, and comprehensive timing data

### 👨‍✈️ Pilot Management
- **Pilot Profiles**: Complete pilot information with photos and catch phrases
- **Performance Analytics**: Track pilot performance across multiple rounds
- **Cross-Race Navigation**: Seamless navigation between race and pilot details
- **Leaderboard Integration**: Full event rankings with point totals

### 📊 Event Organization
- **Event Overview**: Complete event details with club information
- **Round Management**: Organized view of all race rounds
- **Multiple Event Types**: Support for standard races and time trials
- **Club Integration**: Club logos, banners, and branding

### 🎯 User Experience
- **Intuitive Navigation**: Tap-to-navigate between related screens
- **Responsive Design**: Optimized for mobile viewing
- **Smart Placeholders**: Clean interface when data isn't available
- **Visual Hierarchy**: Medal colors, typography, and spacing for clarity

## Technical Stack

- **Framework**: NativeScript 8.7+ with Vue 3
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Icons**: Font Awesome 4.7
- **Platform**: Cross-platform (iOS & Android)

## Project Structure

```
src/
├── views/               # Main application screens
│   ├── Dashboard.vue    # Home dashboard with club selection
│   ├── Explore.vue      # Event discovery and browsing
│   ├── Clubs.vue        # Club listing and management
│   ├── EventDetails.vue # Event overview with tabs
│   ├── RoundDetails.vue # Round/race listing
│   ├── RaceDetails.vue  # Detailed race results
│   └── PilotDetails.vue # Pilot performance analysis
├── components/          # Reusable components
│   └── ClubEventHeader.vue
├── utils/              # Utility functions
│   ├── formatRaceTime.ts
│   ├── sortResults.ts
│   └── storage.ts
├── types/              # TypeScript type definitions
│   └── events.vue.d.ts
└── assets/             # Static assets and images
```

## API Integration

The app integrates with the [FPVTrackside](https://fpvtrackside.com) public API:
- **Base URL**: `https://fpvtrackside.com/api/public/`
- **Endpoints**: Events, rounds, races, pilots, clubs, and timing data
- **Image Handling**: Automatic URL resolution for relative image paths

## Installation & Development

### Prerequisites
- Node.js 16+
- NativeScript CLI 8.7+
- Android SDK (for Android development)
- Xcode (for iOS development)

### Setup
```bash
# Clone the repository
git clone https://github.com/jamiecarl/droneside.git
cd droneside

# Install dependencies
npm install

# Run on Android
ns run android

# Run on iOS
ns run ios
```

### Build for Production
```bash
# Android APK
ns build android --release

# iOS App Store
ns build ios --release
```

## Key Features Implementation

### Smart Data Display
- Conditional rendering prevents "undefined" values
- Graceful fallbacks for missing data
- Loading states with Font Awesome spinner icons

### Image Optimization
- Automatic URL resolution for API images
- Fallback to default images when unavailable
- Proper aspect ratio handling

### Navigation Flow
- **Dashboard** → **Events** → **Rounds** → **Races** → **Pilot Details**
- Cross-references between pilots and races
- Context-aware navigation maintaining user flow

### Performance Features
- Efficient data caching and state management
- Optimized image loading with URL validation
- Smooth scrolling and responsive layouts

## Version History

### v1.0.5 (Current)
- Fixed pilot image loading errors
- Added navigation from race details to pilot details
- Improved pilot header styling (callsign vs real name distinction)
- Simplified event object passing between views
- Hidden empty timing displays when no data available
- Added pilot logos to leaderboard
- Added navigation from leaderboard to pilot details
- Updated timing labels from "Best Lap" to "Best"
- Consistent pilot logo sizing across views

### v1.0.4
- Enhanced UI consistency
- Improved error handling
- Performance optimizations

## Contributing

This project follows standard Vue.js and NativeScript development practices. When contributing:

1. Follow TypeScript strict mode guidelines
2. Maintain consistent component structure
3. Use Tailwind CSS classes for styling
4. Test on both Android and iOS platforms

## License

Private project for FPV drone racing community.

## Contact

For questions or support, please contact [dev@funkynerd.com](mailto:dev@funkynerd.com).

---

*Built with ❤️ for the FPV racing community*