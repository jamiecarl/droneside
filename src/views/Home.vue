<script lang="ts" setup>
import { ref, computed } from 'nativescript-vue';
import Dashboard from './Dashboard.vue';
import Explore from './Explore.vue';
import Clubs from './Clubs.vue';

// Tab navigation state
const activeTab = ref<'home' | 'explore' | 'clubs'>('home');

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

function refreshData() {
  if (activeTab.value === 'explore' && exploreRef.value) {
    exploreRef.value.refreshData();
  } else if (activeTab.value === 'clubs' && clubsRef.value) {
    clubsRef.value.refreshData();
  }
}

// Show refresh button only for tabs that have data to refresh
const showRefreshButton = computed(() => {
  return activeTab.value === 'explore' || activeTab.value === 'clubs';
});
</script>

<template>
  <Frame>
    <Page>
      <ActionBar>
        <Label :text="pageTitle" class="font-bold text-lg" />
        <ActionItem v-if="showRefreshButton" text="Refresh" android.systemIcon="ic_menu_refresh" @tap="refreshData" />
      </ActionBar>

      <GridLayout rows="*, auto">
        <!-- Main Content Area -->
        <ContentView row="0">
          <Dashboard v-if="activeTab === 'home'" />
          <Explore v-else-if="activeTab === 'explore'" ref="exploreRef" />
          <Clubs v-else-if="activeTab === 'clubs'" ref="clubsRef" />
        </ContentView>

        <!-- Bottom Toolbar -->
        <GridLayout row="1" columns="*, *, *" class="bottom-toolbar h-16" orientation="horizontal">
          <!-- Home Tab -->
          <StackLayout col="0" class="tab-item p-3 text-center" :class="activeTab === 'home' ? 'tab-active' : 'tab-inactive'" @tap="setActiveTab('home')">
            <Label text="⌂" class="tab-icon" />
            <Label text="Home" class="tab-text" />
          </StackLayout>

          <!-- Explore Tab -->
          <StackLayout col="1" class="tab-item p-3 text-center" :class="activeTab === 'explore' ? 'tab-active' : 'tab-inactive'" @tap="setActiveTab('explore')">
            <Label text="⌘" class="tab-icon" />
            <Label text="Explore" class="tab-text" />
          </StackLayout>

          <!-- Clubs Tab -->
          <StackLayout col="2" class="tab-item p-3 text-center" :class="activeTab === 'clubs' ? 'tab-active' : 'tab-inactive'" @tap="setActiveTab('clubs')">
            <Label text="◉" class="tab-icon" />
            <Label text="Clubs" class="tab-text" />
          </StackLayout>
        </GridLayout>
      </GridLayout>
    </Page>
  </Frame>
</template>
