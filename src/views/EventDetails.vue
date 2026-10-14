<script lang="ts" setup>
import { ref, onMounted, computed, $navigateTo } from 'nativescript-vue';
import { RoundType, EventType, PilotType, ChannelType, RaceSummaryType } from 'types/events.vue';
import RoundDetails from './RoundDetails.vue';
import PilotDetails from './PilotDetails.vue';
import { Application, GridLayout, ScrollView, StackLayout, TabViewItem } from '@nativescript/core';
import ClubEventHeader from '../components/ClubEventHeader.vue';

const rounds = ref<RoundType[]>([]);
const pilots = ref<PilotType[]>([]);
const raceSummaries = ref<RaceSummaryType[]>([]);
const props = defineProps<{ event: EventType; channels: ChannelType[] }>();

const loadingRounds = ref(true);
const loadingPilots = ref(true);
const loadingRaceSummaries = ref(true);

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
  } finally {
    fetchRounds(eventId);
  }
}

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
    pilots.value = data.sort((a: PilotType, b: PilotType) => a.Name.localeCompare(b.Name));
  } catch (error) {
    console.error('Error fetching pilots:', error);
  } finally {
    loadingPilots.value = false;
  }
}

async function fetchRaceSummaries(eventId: string) {
  try {
    if (pilots.value.length === 0) {
      fetchPilots(eventId);
    }
    console.log('Fetching race summaries for event:', eventId);
    const response = await fetch(`https://fpvtrackside.com/api/public/raceSummaries/eventId/${eventId}`);
    const data = await response.json();
    raceSummaries.value = data;
  } catch (error) {
    console.error('Error fetching race summaries:', error);
  } finally {
    loadingRaceSummaries.value = false;
  }
}

const sortedTopLaps = computed(() => {
  // Get the best lap time for each pilot
  const pilotBestLaps: { [key: string]: RaceSummaryType } = {};

  raceSummaries.value.forEach(summary => {
    const pilotId = summary.pilot_id;
    
    // Check if this pilot doesn't have a best lap yet, or if this lap is better
    if (!pilotBestLaps[pilotId] || 
        (summary.pb_lap_time && summary.pb_lap_time > 0 && 
         (!pilotBestLaps[pilotId].pb_lap_time || summary.pb_lap_time < pilotBestLaps[pilotId].pb_lap_time))) {
      pilotBestLaps[pilotId] = summary;
    }
  });

  return Object.values(pilotBestLaps)
    .filter(summary => summary.pb_lap_time && summary.pb_lap_time > 0) // Only include pilots with valid lap times
    .sort((a, b) => a.pb_lap_time - b.pb_lap_time); // Sort by fastest lap time
});

function getPilotName(pilotId: string): string {
  const pilot = pilots.value.find(pilot => pilot.ID === pilotId);
  return pilot ? pilot.Name : 'Unknown Pilot';
}

function getPilotPhotoURL(pilotId: string): string {
  const pilot = pilots.value.find(pilot => pilot.ID === pilotId);
  if (!pilot || !pilot.PhotoURL) {
    return '~/assets/pilot.png';
  }
  
  // If PhotoURL is a relative path, prepend the base URL
  if (pilot.PhotoURL.startsWith('/')) {
    return 'https://fpvtrackside.com' + pilot.PhotoURL;
  }
  
  return pilot.PhotoURL;
}

function navigateToPilotFromTopLaps(pilotId: string) {
  const pilot = pilots.value.find(p => p.ID === pilotId);
  if (pilot) {
    $navigateTo(PilotDetails, {
      props: {
        pilot,
        eventId: props.event.ID,
        pilots: pilots.value
      }
    });
  }
}

function formatLapTime(seconds: number): string {
  if (!seconds || seconds === 0) return 'N/A';
  return seconds.toFixed(2);
}

function formatDate(dateString: string): string {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
}

function formatGap(pbLapTime: number, index: number): string {
  if (index === 0) return '';
  const fastest = sortedTopLaps.value[0]?.pb_lap_time;
  if (!fastest || !pbLapTime) return '';
  return '+' + (pbLapTime - fastest).toFixed(2);
}

function onTabChange(index: any) {
  if (index.value === 0 && loadingRounds.value) {
    fetchEventDetails(props.event.ID);
  } else if (index.value === 1 && loadingPilots.value) {
    fetchPilots(props.event.ID);
  } else if (index.value === 2 && loadingRaceSummaries.value) {
    fetchRaceSummaries(props.event.ID);
  }
}

onMounted(() => {
  fetchPilots(props.event.ID);
  onTabChange(0); // Load the first tab by default
  Application.on(Application.resumeEvent, () => onTabChange(0));
});
</script>

<template>
  <Page class="bg-ds-base">
    <ActionBar class="bg-ds-base">
      <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
      <Label text="Event" class="font-bold text-lg text-ds-ink" />
      <ActionItem text="Refresh" android.systemIcon="ic_menu_refresh"
        @tap="fetchEventDetails(props.event.ID); fetchPilots(props.event.ID);" />
    </ActionBar>
    <GridLayout rows="auto, *" class="bg-ds-base p-0">
      <StackLayout row="0" class="px-5 pt-1 pb-4">
        <Label :text="props.event.Name" class="text-2xl font-bold text-ds-ink mb-1" textWrap="true" />
        <Label :text="props.event.Club.Name" class="text-sm text-ds-muted2" />
      </StackLayout>
      <ContentView row="1" class="bg-ds-base">
        <TabView class="ds-tabview" @selectedIndexChange="onTabChange">
          <TabViewItem title="Rounds">
            <GridLayout class="bg-ds-base">
              <ScrollView row="1" v-if="!loadingRounds">
                <StackLayout class="p-4 bg-ds-base">
                  <GridLayout v-for="(round, index) in rounds" :key="round.ID"
                    class="p-4 mb-2.5 bg-ds-surface border border-ds-line rounded-2xl"
                    columns="auto, *, auto"
                    @tap="$navigateTo(RoundDetails, { props: { event: props.event, round, pilots, channels: props.channels } })">
                    <Label col="0" :text="round.RoundNumber.toString().padStart(2, '0')"
                      class="font-mono-ds text-2xl font-semibold text-ds-muted mr-3" />
                    <Label col="1" :text="'Round ' + round.RoundNumber" class="text-base font-semibold text-ds-ink2" />
                    <Label col="2" :text="round.EventType"
                      class="font-mono-ds text-[10px] tracking-wide uppercase text-ds-muted border border-ds-line rounded-md px-2 py-1" />
                  </GridLayout>
                </StackLayout>
              </ScrollView>
              <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
            </GridLayout>
          </TabViewItem>
          <TabViewItem title="Pilots">
            <GridLayout class="bg-ds-base">
              <ScrollView row="1" v-if="!loadingPilots && pilots.length > 0">
                <StackLayout class="p-4 bg-ds-base">
                  <GridLayout v-for="(pilot, index) in pilots" :key="pilot.ID"
                    class="p-3.5 mb-2.5 bg-ds-surface border border-ds-line rounded-2xl"
                    columns="auto, *"
                    @tap="$navigateTo(PilotDetails, { props: { pilot, eventId: props.event.ID, pilots } })">
                    <Image v-if="pilot.PhotoURL" :src="pilot.PhotoURL.startsWith('/') ? 'https://fpvtrackside.com' + pilot.PhotoURL : pilot.PhotoURL" col="0"
                      class="h-11 w-11 object-cover rounded-xl mr-3" />
                    <Image v-else src="~/assets/pilot.png" col="0" class="h-11 w-11 object-cover rounded-xl mr-3" />
                    <StackLayout col="1" class="bg-transparent" verticalAlignment="center">
                      <Label :text="pilot.Name" class="text-base font-semibold text-ds-ink" />
                      <Label :text="pilot.FirstName + ' ' + pilot.LastName" v-if="pilot.FirstName || pilot.LastName"
                        class="text-xs text-ds-muted2" />
                      <Label :text="pilot.CatchPhrase" v-if="pilot.CatchPhrase" class="font-mono-ds text-xs text-ds-muted mt-0.5" />
                    </StackLayout>
                  </GridLayout>
                </StackLayout>
              </ScrollView>
              <StackLayout v-else-if="!loadingPilots && pilots.length === 0" class="p-8 text-center bg-ds-base">
                <Label class="text-5xl mb-4 text-ds-faint fa" text="&#xf007;" />
                <Label text="No pilots registered" class="text-lg text-ds-muted mb-2" />
                <Label text="Pilots will appear here once they register for this event" class="text-sm text-ds-muted2" textWrap="true" />
              </StackLayout>
              <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
            </GridLayout>
          </TabViewItem>
          <TabViewItem title="Top Laps">
            <GridLayout class="bg-ds-base">
              <ScrollView row="1" v-if="!loadingRaceSummaries && sortedTopLaps.length > 0">
                <StackLayout class="p-4 bg-ds-base">
                  <GridLayout v-for="(summary, index) in sortedTopLaps" :key="summary.pilot_id"
                    :class="index === 0
                      ? 'p-3.5 mb-1.5 bg-ds-cyan-surface border border-ds-cyan-border rounded-2xl'
                      : 'py-3 px-3.5 border-b border-ds-line-soft'"
                    columns="24, auto, *, auto"
                    @tap="navigateToPilotFromTopLaps(summary.pilot_id)">
                    <Label col="0" :text="(index + 1).toString()"
                      class="font-mono-ds text-sm mr-2" :class="index === 0 ? 'text-ds-cyan' : 'text-ds-muted'" />
                    <Image col="1" :src="getPilotPhotoURL(summary.pilot_id)"
                           :class="index === 0 ? 'h-9 w-9' : 'h-8 w-8'" class="object-cover rounded-xl mr-3" />
                    <Label col="2" :text="getPilotName(summary.pilot_id)" class="text-sm"
                      :class="index === 0 ? 'font-semibold text-ds-ink' : 'text-ds-ink2'" />
                    <StackLayout col="3">
                      <Label :text="formatLapTime(summary.pb_lap_time)" class="font-mono-ds text-right"
                        :class="index === 0 ? 'text-xl font-semibold text-ds-cyan' : 'text-base text-ds-ink2'" />
                      <Label v-if="index > 0" :text="formatGap(summary.pb_lap_time, index)"
                        class="font-mono-ds text-[10px] text-ds-muted2 text-right" />
                    </StackLayout>
                  </GridLayout>
                </StackLayout>
              </ScrollView>
              <StackLayout v-else-if="!loadingRaceSummaries && sortedTopLaps.length === 0" class="p-8 text-center bg-ds-base">
                <Label class="text-5xl mb-4 text-ds-faint fa" text="&#xf017;" />
                <Label text="No lap times available" class="text-lg text-ds-muted mb-2" />
                <Label text="Lap times will appear here once races are completed" class="text-sm text-ds-muted2" textWrap="true" />
              </StackLayout>
              <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
            </GridLayout>
          </TabViewItem>
        </TabView>
      </ContentView>
    </GridLayout>
  </Page>
</template>

<style scoped></style>