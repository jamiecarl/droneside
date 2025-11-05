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
      <Label text="Welcome to DroneSide" class="text-3xl font-bold text-white mb-4" textWrap="true" />
      
      <!-- Getting Started -->
      <StackLayout class="bg-gray-100 p-6 rounded-lg mb-6 mt-8" style="padding-top: 24; padding-bottom: 24;">
        <Label text="Getting Started" class="text-lg font-semibold text-gray-700 mb-6" />
        <Label text="Browse events in the Explore tab or select a club from the Clubs tab to see personalized content here." class="text-base text-gray-600 text-center" textWrap="true" style="line-height: 1.3;" />
      </StackLayout>
      
    </StackLayout>
  </GridLayout>
</template>