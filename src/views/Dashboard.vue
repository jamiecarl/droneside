<script lang="ts" setup>
import { ref } from 'nativescript-vue';

// For now, just showing a placeholder message
// Later this will show events from the user's selected club
const selectedClub = ref<string | null>(null);
</script>

<template>
  <GridLayout rows="*">
    <StackLayout class="p-8 text-center">
      <!-- Welcome Icon -->
      <Label text="⌂" class="text-8xl mb-6 text-blue-500" />
      
      <!-- Welcome Message -->
      <Label text="Welcome to DroneSide" class="text-3xl font-bold text-gray-800 mb-4" textWrap="true" />
      
      <!-- Club Status -->
      <StackLayout class="bg-gray-100 p-6 rounded-lg mb-6">
        <Label text="Club Status" class="text-lg font-semibold text-gray-700 mb-2" />
        <Label text="You do not have a selected club" class="text-base text-gray-600 mb-3" textWrap="true" />
        <Label text="Select a club from the Clubs tab to see your home events and personalized content here." class="text-sm text-gray-500 text-center" textWrap="true" />
      </StackLayout>
      
      <!-- Quick Action Hint -->
      <StackLayout class="bg-blue-50 p-4 rounded-lg">
        <Label text="💡 Getting Started" class="text-base font-semibold text-blue-700 mb-2" />
        <Label text="Browse all events in the Explore tab or find clubs in the Clubs tab to get started!" class="text-sm text-blue-600 text-center" textWrap="true" />
      </StackLayout>
    </StackLayout>
  </GridLayout>
</template>