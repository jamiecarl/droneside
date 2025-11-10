<script lang="ts" setup>
import { computed } from 'nativescript-vue';
import { GridLayout, StackLayout } from '@nativescript/core';
import { defineProps, ref, onMounted } from 'nativescript-vue';
import type { EventType, PilotType, RoundType, RawRaceDataType, RaceDetailType, LapType } from 'types/events.vue';
import { formatRaceTime } from '~/utils/formatRaceTime';
import RaceDetails from './RaceDetails.vue';
import { get } from 'http';

const props = defineProps<{ pilot: PilotType, event: EventType, pilots: PilotType[] }>();
const loadingDetails = ref(true);
const rounds = ref<{ [key: string]: RoundType }>({});
const roundDetails = ref<{ [key: string]: RaceDetailType }>({});
const raceSummaries = ref<{ [key: string]: any }>({});
const rawRaceDetails = ref<{ [key: string]: RawRaceDataType }>({});

const raceSummariesCount = computed(() => Object.keys(raceSummaries.value).length);

async function fetchRounds(eventId: string) {
  try {
    console.log('Fetching rounds for event:', eventId);
    const response = await fetch(`https://fpvtrackside.com/api/public/rounds/eventId/${eventId}`);
    const data = await response.json();
    const indexedRounds = data.reduce((acc: { [key: string]: RaceDetailType }, round: RaceDetailType) => {
      acc[round.ID] = round;
      return acc;
    }, {});
    rounds.value = indexedRounds;
  } catch (error) {
    console.error('Error fetching rounds:', error);
  }
}

async function fetchPilotRaces(eventId: string, pilotId: string) {
  try {
    console.log('Fetching races for pilot:', pilotId, 'event:', eventId);
    const response = await fetch(`https://fpvtrackside.com/api/public/races/eventId/${eventId}/pilotId/${pilotId}`);
    const data = await response.json();
    const indexedRounds = data.reduce((acc: { [key: string]: RaceDetailType }, race: RaceDetailType) => {
      acc[race.Round] = race;
      return acc;
    }, {});
    const summaries = data.reduce((acc: { [key: string]: any }, race: any) => {
      const summary = race.ResultSummaries.find((summary: any) => summary.Pilot === pilotId);
      if (summary) {
        acc[race.Round] = summary;
      }
      return acc;
    }, {});
    roundDetails.value = indexedRounds;
    raceSummaries.value = summaries;
  } catch (error) {
    console.error('Error fetching pilot races:', error);
  }
}

async function fetchRawRaceDetail(eventId: string, raceId: string) {
  try {
    const response = await fetch(`https://fpvtrackside.com/api/public/races/eventId/${eventId}/raceId/${raceId}/raw/1`);
    const data = await response.json();
    rawRaceDetails.value[raceId] = data;
  } catch (error) {
    console.error('Error fetching raw race detail:', error);
  }
}

function getLapsForPilot(raceId: string, pilotId: string): LapType[] {
  if (!rawRaceDetails.value) return [];
  const rawRace = rawRaceDetails.value[raceId];
  if (!rawRace || !rawRace.Laps || !rawRace.Detections) return [];
  const pilotDetectionIds = rawRace.Detections
    .filter(detection => detection.Pilot === pilotId && detection.Valid !== false && detection.LapNumber > 0)
    .map(detection => detection.ID);
  return rawRace.Laps
    .filter(lap => pilotDetectionIds.includes(lap.Detection))
    .sort((a, b) => a.LapNumber - b.LapNumber);
}

function refreshData() {
  fetchRounds(props.event.ID).then(() => {
    fetchPilotRaces(props.event.ID, props.pilot.ID).then(() => {
      loadingDetails.value = false;
      const raceIds = Object.values(raceSummaries.value).map((summary: any) => summary.Race);
      Promise.all(raceIds.map(raceId => fetchRawRaceDetail(props.event.ID, raceId)));
    });
  });
}

function getRaceById(roundId: string): RaceDetailType {
  return roundDetails.value[roundId];
}

function getRoundById(raceId: string): RoundType {
  return rounds.value[raceId];
}

onMounted(() => {
  loadingDetails.value = true;
  refreshData();
});
</script>

<template>
  <Page>
    <ActionBar>
      <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
      <Label text="Pilot Details" class="font-bold text-lg" />
      <ActionItem text="Refresh" android.systemIcon="ic_menu_refresh" @tap="refreshData" />
    </ActionBar>
    <GridLayout rows="auto, *">
      <!-- Fixed header section -->
      <GridLayout row="0" columns="auto, *" class="p-3 bg-white">
        <Image col="0" v-if="pilot.PhotoURL" :src="pilot.PhotoURL" class="h-16 w-16 object-cover rounded-lg mr-3" />
        <Image col="0" v-else src="~/assets/pilot.png" class="h-16 w-16 object-cover rounded-lg mr-3" />
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
        <StackLayout class="p-3 bg-black">
          <StackLayout v-for="round in rounds" :key="round.ID" class="p-4 my-2 bg-gray-800 rounded-md"
            @tap="$navigateTo(RaceDetails, { props: { race: getRaceById(round.ID), pilots: props.pilots, round: getRoundById(round.ID) } })">
            <GridLayout columns="*" class="bg-transparent">
              <StackLayout col="0">
                <!-- Round title and position/points header -->
                <GridLayout columns="*, auto" class="bg-transparent">
                  <StackLayout col="0" verticalAlignment="top">
                    <Label :text="'Round #' + round.RoundNumber" class="text-white text-xl font-bold" />
                    <Label :text="raceSummaries[round.ID]?.LapCount + ' laps'" class="text-gray-400 text-sm" />
                  </StackLayout>
                  <GridLayout col="1" rows="auto, auto" class="bg-transparent">
                    <Label v-if="round.EventType !== 'TimeTrial'" row="0" width="40" height="40"
                      :text="raceSummaries[round.ID]?.Position || 'NA'"
                      class="text-xl font-bold text-black text-center rounded-md"
                      :class="'medal-' + (raceSummaries[round.ID]?.Position || '8')" />
                    <Label v-if="parseInt(raceSummaries[round.ID]?.Points) > 0" row="1"
                      :text="raceSummaries[round.ID]?.Points + ' points'" class="text-white text-xs" />
                  </GridLayout>
                </GridLayout>
                
                <!-- Race timing display - single row layout -->
                <GridLayout v-if="round.EventType !== 'TimeTrial'" columns="*, *, *" class="bg-transparent mt-4 mb-5 text-xs">
                  <StackLayout col="0" class="bg-transparent text-center">
                    <Label text="Holeshot" class="text-gray-400 mb-1 text-base" />
                    <Label :text="formatRaceTime(raceSummaries[round.ID]?.HoleshotTime || 'NA')" class="text-yellow-400 text-base" />
                  </StackLayout>
                  <StackLayout col="1" class="bg-transparent text-center">
                    <Label text="Best" class="text-gray-400 mb-1 text-base" />
                    <Label :text="formatRaceTime(raceSummaries[round.ID]?.PbLapTime || 'NA')" class="text-green-500 text-base" />
                  </StackLayout>
                  <StackLayout col="2" class="bg-transparent text-center">
                    <Label text="Race" class="text-gray-400 mb-1 text-base" />
                    <Label :text="formatRaceTime(raceSummaries[round.ID]?.RaceTime || (raceSummaries[round.ID]?.Position?'DNF':'-'))" class="text-white text-base" />
                  </StackLayout>
                </GridLayout>
                <!-- TimeTrial layout - only shows Best -->
                <StackLayout v-else class="bg-transparent mt-4 mb-5 text-xs text-center">
                  <Label text="Best" class="text-gray-400 mb-1 text-base" />
                  <Label :text="formatRaceTime(raceSummaries[round.ID]?.PbLapTime || 'NA')" class="text-green-500 text-base" />
                </StackLayout>
              </StackLayout>
            </GridLayout>
            <!-- No laps placeholder -->
            <StackLayout v-if="raceSummaries[round.ID]?.LapCount === 0" class="p-4 text-center">
              <Label class="fa text-gray-500 text-4xl mb-2 text-center" text="&#xf017;" />
              <Label text="No laps recorded" class="text-gray-400 text-base text-center mb-1" />
              <Label text="This pilot hasn't completed any valid laps yet" class="text-gray-500 text-sm text-center" />
            </StackLayout>
            <GridLayout v-else-if="getLapsForPilot(raceSummaries[round.ID]?.Race, props.pilot.ID).length > 0"
              :rows="`auto${', auto'.repeat(raceSummaries[round.ID]?.LapCount)}`" columns="*, *" 
              class="bg-gray-900 rounded-md p-3 mt-3">
              <!-- Header Row -->
              <Label row="0" col="0" text="Lap" class="text-white font-bold text-sm pb-2"
                style="border-bottom-width: 1px; border-bottom-color: rgba(255,255,255,0.2);" />
              <Label row="0" col="1" text="Time" class="text-white font-bold text-sm pb-2 text-right"
                style="border-bottom-width: 1px; border-bottom-color: rgba(255,255,255,0.2);" />
              <!-- Data Rows -->
              <template v-for="(lap, index) in getLapsForPilot(raceSummaries[round.ID]?.Race, props.pilot.ID)"
                :key="lap.ID">
                <Label :row="index + 1" col="0" :text="'#' + lap.LapNumber" 
                    class="text-gray-300 py-1 text-sm" />
                <Label :row="index + 1" col="1" :text="lap.LengthSeconds.toFixed(2)"
                    class="text-white py-1 text-sm text-right font-mono" />
              </template>
            </GridLayout>
            <!-- No data placeholder -->
            <StackLayout v-else-if="!(raceSummaries[round.ID]?.LapCount > 0)" class="p-4 text-center">
              <Label class="fa text-gray-500 text-4xl mb-2 text-center" text="&#xf017;" />
              <Label text="No data available" class="text-gray-400 text-base text-center mb-1" />
              <Label text="Race data is not available for this round" class="text-gray-500 text-sm text-center" />
            </StackLayout>
            <!-- Loading placeholder -->
            <StackLayout v-else class="p-4 text-center">
              <Label class="fa text-gray-500 text-4xl mb-2 text-center" text="&#xf110;" />
              <Label text="Loading lap times..." class="text-gray-400 text-base text-center mb-1" />
              <Label text="Please wait while we fetch the data" class="text-gray-500 text-sm text-center" />
            </StackLayout>
          </StackLayout>
        </StackLayout>
      </ScrollView>
      <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
    </GridLayout>
  </Page>
</template>
