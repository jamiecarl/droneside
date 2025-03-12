<script lang="ts" setup>
import { ref, onMounted, computed } from 'nativescript-vue';
import { RoundType, EventType, PilotType, ChannelType, PointsType } from 'types/events.vue';
import RoundDetails from './RoundDetails.vue';
import PilotDetails from './PilotDetails.vue';
import { Application, GridLayout, ScrollView, StackLayout, TabViewItem } from '@nativescript/core';
import ClubEventHeader from '../components/ClubEventHeader.vue';

const rounds = ref<RoundType[]>([]);
const pilots = ref<PilotType[]>([]);
const points = ref<PointsType[]>([]);
const props = defineProps<{ event: EventType; channels: ChannelType[] }>();

const loadingRounds = ref(true);
const loadingPilots = ref(true);
const loadingPoints = ref(true);

async function fetchEventDetails(eventId: string) {
  try {
    console.log('Fetching event details:', eventId);
    const response = await fetch(`https://fpvtrackside.com/api/public/events/id/${eventId}`);
    const data = await response.json();
    // Update channels with color information
    if (data.Channels) {
      props.channels.forEach(channel => {
        const eventChannel = data.Channels.find((c: any) => c.Channel === channel.ID);
        if (eventChannel) {
          channel.Color = eventChannel.Color;
        }
      });
    }
  } catch (error) {
    console.error('Error fetching event details:', error);
  }
}

async function fetchRounds(eventId: string) {
  try {
    await fetchEventDetails(eventId);
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
    pilots.value = data.sort((a: PilotType, b: PilotType) => a.Name.localeCompare(b.Name));
  } catch (error) {
    console.error('Error fetching pilots:', error);
  } finally {
    loadingPilots.value = false;
  }
}

async function fetchPoints(eventId: string) {
  try {
    if (pilots.value.length === 0) {
      fetchPilots(eventId);
    }
    console.log('Fetching points for event:', eventId);
    const response = await fetch(`https://fpvtrackside.com/api/public/points/eventId/${eventId}`);
    const data = await response.json();
    points.value = data;
  } catch (error) {
    console.error('Error fetching points:', error);
  } finally {
    loadingPoints.value = false;
  }
}

const sortedPoints = computed(() => {
  const pilotPointsMap: { [key: string]: number } = {};

  points.value.forEach(point => {
    if (!pilotPointsMap[point.pilot_id]) {
      pilotPointsMap[point.pilot_id] = 0;
    }
    pilotPointsMap[point.pilot_id] += parseInt(point.points);
  });

  return Object.entries(pilotPointsMap)
    .map(([PilotID, totalPoints]) => ({ PilotID, Points: totalPoints }))
    .sort((a, b) => b.Points - a.Points);
});

function getPilotName(pilotId: string): string {
  const pilot = pilots.value.find(pilot => pilot.ID === pilotId);
  return pilot ? pilot.Name : 'Unknown Pilot';
}

function formatDate(dateString: string): string {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
}

function onTabChange(index: any) {
  if (index.value === 0 && loadingRounds.value) {
    fetchRounds(props.event.ID);
  } else if (index.value === 1 && loadingPilots.value) {
    fetchPilots(props.event.ID);
  } else if (index.value === 2 && loadingPoints.value) {
    fetchPoints(props.event.ID);
  }
}

onMounted(() => {
  fetchPilots(props.event.ID);
  onTabChange(0); // Load the first tab by default
  Application.on(Application.resumeEvent, () => onTabChange(0));
});
</script>

<template>
  <Page>
    <ActionBar>
      <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
      <Label text="Event Rounds" class="font-bold text-lg" />
      <ActionItem text="Refresh" android.systemIcon="ic_menu_refresh"
        @tap="fetchRounds(props.event.ID); fetchPilots(props.event.ID);" />
    </ActionBar>
    <GridLayout rows="auto, *" class="p-0">
      <ClubEventHeader row="0" :event="props.event" :formatDate="formatDate" />
      <ContentView row="1" class="bg-black rounded-t-3xl">
        <TabView @selectedIndexChange="onTabChange">
          <TabViewItem title="Rounds">
            <GridLayout>
              <ScrollView row="1" v-if="!loadingRounds">
                <StackLayout class="p-3 bg-black">
                  <GridLayout v-for="(round, index) in rounds" :key="round.ID" class="p-4 my-2 bg-gray-800 rounded-md"
                    columns="*, auto"
                    @tap="$navigateTo(RoundDetails, { props: { event: props.event, round, pilots, channels: props.channels } })">
                    <Label col="0" :text="'Round #' + round.RoundNumber" class="text-3xl text-white" />
                    <Label col="1" :text="round.EventType" class="text-xl text-red-500 text-right" />
                  </GridLayout>
                </StackLayout>
              </ScrollView>
              <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
            </GridLayout>
          </TabViewItem>
          <TabViewItem title="Pilots">
            <GridLayout>
              <ScrollView row="1" v-if="!loadingPilots">
                <StackLayout class="p-3 bg-black">
                  <GridLayout v-for="(pilot, index) in pilots" :key="pilot.ID" class="p-4 my-2 bg-gray-800 rounded-md"
                    columns="auto, *"
                    @tap="$navigateTo(PilotDetails, { props: { pilot, event: props.event, pilots } })">
                    <Image v-if="pilot.PhotoURL" :src="pilot.PhotoURL" col="0"
                      class="h-16 w-16 object-cover rounded-lg mr-3" />
                    <Image v-else src="~/assets/pilot.png" col="0" class="h-16 w-16 object-cover rounded-lg mr-3" />
                    <StackLayout col="1" class="bg-transparent">
                      <Label :text="pilot.Name" class="text-2xl text-white" />
                      <Label :text="pilot.FirstName + ' ' + pilot.LastName" v-if="pilot.FirstName || pilot.LastName"
                        class="text-gray-500" />
                      <Label :test="pilot.CatchPhrase" v-if="pilot.CatchPhrase" class="text-gray-300" />
                    </StackLayout>
                  </GridLayout>
                </StackLayout>
              </ScrollView>
              <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
            </GridLayout>
          </TabViewItem>
          <TabViewItem title="Leaderboard">
            <GridLayout>
              <ScrollView row="1" v-if="!loadingPoints">
                <StackLayout class="p-3 bg-black">
                  <GridLayout v-for="(point, index) in sortedPoints" :key="point.PilotID"
                    class="p-4 my-2 bg-gray-800 rounded-md" columns="*, auto">
                    <Label col="0" :text="getPilotName(point.PilotID)" class="text-lg text-white" />
                    <Label col="1" :text="point.Points" class="text-lg text-red-500 text-right" />
                  </GridLayout>
                </StackLayout>
              </ScrollView>
              <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
            </GridLayout>
          </TabViewItem>
        </TabView>
      </ContentView>
    </GridLayout>
  </Page>
</template>

<style scoped></style>