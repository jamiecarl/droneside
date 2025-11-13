<script lang="ts" setup>
import { FlexboxLayout, GridLayout, StackLayout } from '@nativescript/core';
import { defineProps, $navigateTo } from 'nativescript-vue';
import { EventType } from 'types/events.vue';
import ClubDetails from '~/views/ClubDetails.vue';

const props = defineProps<{ 
  event: EventType; 
  formatDate: (date: string) => string;
  enableClubNavigation?: boolean;
}>();

function goToClubDetails() {
  if (props.enableClubNavigation) {
    $navigateTo(ClubDetails, { props: { clubId: props.event.Club.ID } });
  }
}

function getClubLogoUrl() {
  if (!props.event.Club.LogoUrl) {
    return 'https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png';
  }
  
  // If LogoUrl is a relative path, prepend the base URL
  if (props.event.Club.LogoUrl.startsWith('/')) {
    return 'https://fpvtrackside.com' + props.event.Club.LogoUrl;
  }
  
  return props.event.Club.LogoUrl;
}
</script>

<template>
  <GridLayout columns="auto, *" class="bg-white p-4">
    <Image col="0" :src="getClubLogoUrl()" 
           class="h-24 w-24 object-cover rounded-lg mr-3" 
           @tap="goToClubDetails" />
    <FlexboxLayout col="1" class="bg-transparent" flexDirection="column" justifyContent="space-between">
      <StackLayout flexGrow="1">
        <Label :text="props.event.Name" class="text-md font-bold text-black" textWrap="true"/>
      </StackLayout>
      <StackLayout>
        <Label :text="props.event.Club.Name" class="text-sm text-gray-400" />
        <GridLayout columns="*, auto" class="bg-transparent">
          <Label col="0" :text="formatDate(props.event.Start)" class="text-sm text-gray-400" />
          <Label col="1" :text="'Pilots: ' + props.event.PilotCount" class="text-sm text-gray-400 text-right" />
        </GridLayout>
      </StackLayout>
    </FlexboxLayout>
  </GridLayout>
</template>
