<script lang="ts" setup>
import {
  ref,
  onMounted,
  getCurrentInstance
} from 'nativescript-vue';
import { EventRoundType, EventType, PilotType } from 'types/events.vue';
import RoundDetails from './RoundDetails.vue';
import PilotDetails from './PilotDetails.vue';
import { TabViewItem } from '@nativescript/core';

const rounds = ref<EventRoundType[]>([]);
const pilots = ref<PilotType[]>([]);
const props = defineProps<{ event: EventType }>();

const loadingRounds = ref(true);
const loadingPilots = ref(true);

async function fetchRounds(eventId: string) {
  try {
    console.log('Fetching rounds for event:', eventId);
    const response = await fetch(`https://fpvtrackside.com/api/public/rounds/eventId/${eventId}`);
    const data = await response.json();
    rounds.value = data;
  } catch (error) {
    console.error('Error fetching rounds:', error);
  } finally {
    loadingRounds.value = false;
  }
}

async function fetchPilots(eventId: string) {
  try {
    console.log('Fetching pilots for event:', eventId);
    const response = await fetch(`https://fpvtrackside.com/api/public/pilots/eventId/${eventId}`);
    const data = await response.json();
    pilots.value = data;
  } catch (error) {
    console.error('Error fetching pilots:', error);
  } finally {
    loadingPilots.value = false;
  }
}

function formatDate(dateString: string): string {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
}

onMounted(() => {
  fetchRounds(props.event.ID);
  fetchPilots(props.event.ID);
});
</script>

<template>
  <Page>
    <ActionBar>
      <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
      <Label text="Event Rounds" class="font-bold text-lg" />
    </ActionBar>
    <GridLayout rows="auto, *" class="p-0">
      <GridLayout row="0" columns="auto, *" class="bg-white p-4">
        <Image col="0" :src="props.event.Club.LogoUrl" class="h-24 w-24 object-cover rounded-lg mr-3" />
        <StackLayout col="1">
          <Label :text="props.event.Name" class="text-lg font-bold text-black" />
          <Label :text="props.event.Club.Name" class="text-sm text-gray-500" />
          <Label :text="formatDate(props.event.Start)" class="text-sm text-gray-500" />
          <Label :text="'Pilots: ' + props.event.PilotCount" class="text-sm text-gray-500" />
        </StackLayout>
      </GridLayout>
      <ContentView row="1" class="bg-black rounded-t-3xl">
        <TabView>
          <TabViewItem title="Rounds">
            <GridLayout>
              <ListView v-if="!loadingRounds" :items="rounds" separatorColor="transparent" class="bg-transparent">
                <template #default="{ item: round }">
                  <GridLayout columns="*, auto" class="px-4 py-2 border-b border-gray-400"
                    @tap="$navigateTo(RoundDetails, { props: { round } })">
                    <StackLayout col="0">
                      <Label :text="'Round #' + round.RoundNumber" class="text-3xl py-3 text-white" />
                      <Label :text="round.EventType" class="text-xl text-red-800" />
                    </StackLayout>
                  </GridLayout>
                </template>
              </ListView>
              <Label v-else text="Loading rounds..." class="text-center text-white" />
            </GridLayout>
          </TabViewItem>
          <TabViewItem title="Pilots">
            <GridLayout>
              <ListView v-if="!loadingPilots" :items="pilots" separatorColor="transparent" class="bg-transparent">
                <template #default="{ item: pilot }">
                  <GridLayout columns="auto, *" class="px-4 py-2 border-b border-gray-400"
                    @tap="$navigateTo(PilotDetails, { props: { pilot } })">
                    <Image
                      v-if="pilot.PhotoURL"
                      :src="pilot.PhotoURL"
                      col="0"
                      class="h-16 w-16 object-cover rounded-lg mr-2"
                    />
                    <Image
                      v-else
                      src="~/assets/pilot.png"
                      col="0"
                      class="h-16 w-16 object-cover rounded-lg mr-2"
                    />
                    <Label col="1" :text="pilot.Name" class="text-3xl py-3 text-white" />
                  </GridLayout>
                </template>
              </ListView>
              <Label v-else text="Loading pilots..." class="text-center text-white" />
            </GridLayout>
          </TabViewItem>
        </TabView>
      </ContentView>
    </GridLayout>
  </Page>
</template>

<style scoped></style>