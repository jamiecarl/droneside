<script lang="ts" setup>
import {
  ref,
  onMounted,
  getCurrentInstance
} from 'nativescript-vue';
import { RoundType, EventType, PilotType, ChannelType } from 'types/events.vue';
import RoundDetails from './RoundDetails.vue';
import PilotDetails from './PilotDetails.vue';
import { GridLayout, TabViewItem } from '@nativescript/core';
import ClubEventHeader from '../components/ClubEventHeader.vue';

const rounds = ref<RoundType[]>([]);
const pilots = ref<PilotType[]>([]);
const props = defineProps<{ event: EventType; channels: ChannelType[] }>();

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
      <ClubEventHeader row="0" :event="props.event" :formatDate="formatDate" />
      <ContentView row="1" class="bg-black rounded-t-3xl">
        <TabView>
          <TabViewItem title="Rounds">
            <GridLayout>
              <ListView v-if="!loadingRounds" :items="rounds" separatorColor="transparent" class="bg-transparent">
                <template #default="{ item: round }">
                  <GridLayout class="p-3 bg-transparent">
                    <GridLayout columns="*, auto" class="p-3 bg-gray-800 rounded-md"
                      @tap="$navigateTo(RoundDetails, { props: { event: props.event, round, pilots, channels: props.channels } })">
                      <Label col="0" :text="'Round #' + round.RoundNumber" class="text-3xl py-3 text-white" />
                      <Label col="1" :text="round.EventType" class="text-xl text-red-500 text-right" />
                    </GridLayout>
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
                    <Image v-if="pilot.PhotoURL" :src="pilot.PhotoURL" col="0"
                      class="h-16 w-16 object-cover rounded-lg mr-2" />
                    <Image v-else src="~/assets/pilot.png" col="0" class="h-16 w-16 object-cover rounded-lg mr-2" />
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