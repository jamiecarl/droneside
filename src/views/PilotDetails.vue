<script lang="ts" setup>
import { GridLayout, StackLayout } from '@nativescript/core';
import { defineProps, ref, onMounted } from 'nativescript-vue';
import type { EventType, PilotType } from 'types/events.vue';

const props = defineProps<{ pilot: PilotType, event: EventType }>();
const loadingDetails = ref(true);

onMounted(() => {
});
</script>

<template>
  <Page>
    <ActionBar>
      <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
      <Label text="Pilot Details" class="font-bold text-lg" />
    </ActionBar>
    <GridLayout rows="auto, *">
      <!-- Fixed header section -->
      <GridLayout row="0" columns="auto, *" class="p-3 bg-white">
        <Image col="0" v-if="pilot.PhotoURL" :src="pilot.PhotoURL" class="h-32 w-32 object-cover rounded-lg mr-3" />
        <Image col="0" v-else src="~/assets/pilot.png" class="h-32 w-32 object-cover rounded-lg mr-3" />
        <StackLayout col="1" class="bg-transparent">
          <Label :text="props.pilot.Name" class="text-black font-bold text-2xl" />
          <Label v-if="props.pilot.FirstName || props.pilot.LastName"
            :text="props.pilot.FirstName + ' ' + props.pilot.LastName" class="text-black font-bold text-2xl" />
          <Label v-if="props.pilot.CatchPhrase" :text="props.pilot.CatchPhrase" class="text-gray-500" />
          <label :text="props.event.Name" class="text-gray-400 text-md" />
        </StackLayout>
      </GridLayout>
      <!-- Scrollable content -->
      <ScrollView row="1" v-if="!loadingDetails">
        <StackLayout>
          <Label text="Pilot Details" class="text-center text-lg font-bold" />
          <Label text="Coming soon..." class="text-center text-lg" />
        </StackLayout>
      </ScrollView>
    </GridLayout>
  </Page>
</template>
