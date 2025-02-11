<script lang="ts" setup>
import { defineProps } from 'nativescript-vue';
import { EventType } from 'types/events.vue';

const props = defineProps<{ event: EventType; formatDate: (date: string) => string }>();
</script>

<template>
  <GridLayout columns="auto, *" class="bg-white p-4">
    <Image col="0" :src="props.event.Club.LogoUrl" class="h-24 w-24 object-cover rounded-lg mr-3" />
    <StackLayout col="1">
      <Label :text="props.event.Name" class="text-lg font-bold text-black" />
      <Label :text="props.event.Club.Name" class="text-sm text-gray-500" />
      <Label :text="formatDate(props.event.Start)" class="text-sm text-gray-500" />
      <Label :text="'Pilots: ' + props.event.PilotCount" class="text-sm text-gray-500" />
    </StackLayout>
  </GridLayout>
</template>
