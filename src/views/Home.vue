<script lang="ts" setup>
import { ref, computed, onMounted } from 'nativescript-vue';
import { Application, Device, Screen } from '@nativescript/core';
import Dashboard from './Dashboard.vue';
import Explore from './Explore.vue';
import Clubs from './Clubs.vue';

// Tab navigation state
const activeTab = ref<'home' | 'explore' | 'clubs'>('home');

// Safe area inset for bottom navigation
const bottomSafeAreaInset = ref(0);

// Function to calculate navigation bar height
function calculateBottomInset() {
  try {
    if(android.os.Build.VERSION.SDK_INT < 35) {
      bottomSafeAreaInset.value = 0;
      return true;
    }
    const activity = Application.android.foregroundActivity || Application.android.startActivity;
    if (activity) {
      // Get navigation bar height from Android system resources
      const resources = activity.getResources();
      const resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
      if (resourceId > 0) {
        bottomSafeAreaInset.value = resources.getDimensionPixelSize(resourceId);
        return true;
      } else {
        // Fallback based on Android version
        const apiLevel = android.os.Build.VERSION.SDK_INT;
        bottomSafeAreaInset.value = apiLevel >= 30 ? 80 : 0;
        return false;
      }
    }
  } catch (error) {
    console.log('Error calculating bottom inset:', error);
    bottomSafeAreaInset.value = 60; // Conservative fallback
    return false;
  }
}

// Try multiple times to get the inset value
function initializeBottomInset() {
  // Try immediately
  if (calculateBottomInset()) {
    return;
  }
  
  // Try after a short delay
  setTimeout(() => {
    if (calculateBottomInset()) {
      return;
    }
    
    // Try after app is fully loaded
    setTimeout(() => {
      calculateBottomInset();
    }, 500);
  }, 100);
}

// Initialize on mount
onMounted(() => {
  initializeBottomInset();
});

// Also listen for application events
Application.on(Application.resumeEvent, () => {
  calculateBottomInset();
});

// Initialize immediately when script loads (fallback)
initializeBottomInset();

// Tab navigation functions
function setActiveTab(tab: 'home' | 'explore' | 'clubs') {
  activeTab.value = tab;
}

// Dynamic title based on active tab
const pageTitle = computed(() => {
  switch (activeTab.value) {
    case 'home':
      return 'DroneSide';
    case 'explore':
      return 'Explore Events';
    case 'clubs':
      return 'Clubs';
    default:
      return 'DroneSide';
  }
});

// Get reference to child components for refresh functionality
const exploreRef = ref();
const clubsRef = ref();
const dashboardRef = ref();

function refreshData() {
  if (activeTab.value === 'home' && dashboardRef.value) {
    dashboardRef.value.refreshData();
  } else if (activeTab.value === 'explore' && exploreRef.value) {
    exploreRef.value.refreshData();
  } else if (activeTab.value === 'clubs' && clubsRef.value) {
    clubsRef.value.refreshData();
  }
}

// Show refresh button for all tabs
const showRefreshButton = computed(() => {
  return true;
});

// Computed style for bottom toolbar with safe area
const bottomToolbarStyle = computed(() => {
  return {
    paddingBottom: `${12 + bottomSafeAreaInset.value}px`
  };
});
</script>

<template>
  <Frame>
    <Page>
      <ActionBar>
        <Label :text="pageTitle" class="font-bold text-lg" />
        <ActionItem v-if="showRefreshButton" text="Refresh" android.systemIcon="ic_menu_refresh" @tap="refreshData" />
      </ActionBar>

      <GridLayout rows="*, auto, auto">
        <!-- Main Content Area -->
        <ContentView row="0">
          <Dashboard v-if="activeTab === 'home'" ref="dashboardRef" />
          <Explore v-else-if="activeTab === 'explore'" ref="exploreRef" />
          <Clubs v-else-if="activeTab === 'clubs'" ref="clubsRef" />
        </ContentView>

        <!-- Toolbar Separator -->
        <StackLayout row="1" class="toolbar-separator"></StackLayout>

        <!-- Bottom Toolbar -->
        <GridLayout row="2" columns="*, *, *" class="bottom-toolbar" :style="bottomToolbarStyle" orientation="horizontal">
          <!-- Home Tab -->
          <StackLayout col="0" class="tab-item text-center" :class="activeTab === 'home' ? 'tab-active' : 'tab-inactive'" @tap="setActiveTab('home')">
            <Label class="tab-icon fa" text="&#xf015;" />
            <Label text="Home" class="tab-text" />
          </StackLayout>

          <!-- Explore Tab -->
          <StackLayout col="1" class="tab-item text-center" :class="activeTab === 'explore' ? 'tab-active' : 'tab-inactive'" @tap="setActiveTab('explore')">
            <Label class="tab-icon fa" text="&#xf002;" />
            <Label text="Explore" class="tab-text" />
          </StackLayout>

          <!-- Clubs Tab -->
          <StackLayout col="2" class="tab-item text-center" :class="activeTab === 'clubs' ? 'tab-active' : 'tab-inactive'" @tap="setActiveTab('clubs')">
            <Label class="tab-icon fa" text="&#xf0c0;" />
            <Label text="Clubs" class="tab-text" />
          </StackLayout>
        </GridLayout>
      </GridLayout>
    </Page>
  </Frame>
</template>
