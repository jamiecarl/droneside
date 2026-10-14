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
  <GridLayout columns="auto, *" class="bg-ds-surface border border-ds-line rounded-2xl p-3.5">
    <Image col="0" :src="getClubLogoUrl()"
           class="h-10 w-10 object-cover rounded-xl mr-3"
           @tap="goToClubDetails" />
    <FlexboxLayout col="1" class="bg-transparent" flexDirection="column" justifyContent="space-between">
      <StackLayout flexGrow="1">
        <Label :text="props.event.Name" class="text-base font-semibold text-ds-ink mb-0.5" textWrap="true"/>
        <Label :text="props.event.Club.Name" class="text-xs text-ds-muted2" textWrap="true" />
      </StackLayout>
      <GridLayout columns="*, auto" class="bg-transparent mt-2">
        <Label col="0" :text="formatDate(props.event.Start)" class="font-mono-ds text-xs text-ds-cyan" />
        <Label col="1" :text="'PILOTS ' + props.event.PilotCount" class="font-mono-ds text-xs text-ds-muted text-right" />
      </GridLayout>
    </FlexboxLayout>
  </GridLayout>
</template>
