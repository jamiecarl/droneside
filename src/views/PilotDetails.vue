<script lang="ts" setup>
import { computed } from 'nativescript-vue';
import { GridLayout, StackLayout } from '@nativescript/core';
import { defineProps, ref, onMounted } from 'nativescript-vue';
import type { PilotType, RoundType, RawRaceDataType, RaceDetailType, LapType } from 'types/events.vue';
import { formatRaceTime } from '~/utils/formatRaceTime';
import RaceDetails from './RaceDetails.vue';
import LapSparkline from '../components/LapSparkline.vue';

const props = defineProps<{ pilot: PilotType, eventId: string, pilots: PilotType[] }>();
const loadingDetails = ref(true);
const rounds = ref<{ [key: string]: RoundType }>({});
const roundDetails = ref<{ [key: string]: RaceDetailType }>({});
const raceSummaries = ref<{ [key: string]: any }>({});
const rawRaceDetails = ref<{ [key: string]: RawRaceDataType }>({});

const raceSummariesCount = computed(() => Object.keys(raceSummaries.value).length);

const overallBestLap = computed(() => {
  const times = Object.values(raceSummaries.value)
    .map((summary: any) => parseFloat(summary.PbLapTime))
    .filter((time) => !isNaN(time) && time > 0);
  return times.length > 0 ? Math.min(...times).toFixed(2) : null;
});

const totalPoints = computed(() => {
  return Object.values(raceSummaries.value).reduce((sum, summary: any) => {
    const points = parseInt(summary.Points);
    return sum + (isNaN(points) ? 0 : points);
  }, 0);
});

// Every lap this pilot has flown across the event, in round order — for the aggregate sparkline.
const allLapsThisEvent = computed(() => {
  return Object.values(rounds.value)
    .sort((a: RoundType, b: RoundType) => parseInt(a.RoundNumber) - parseInt(b.RoundNumber))
    .flatMap((round: RoundType) => {
      const summary = raceSummaries.value[round.ID];
      if (!summary) return [];
      return getLapsForPilot(summary.Race, props.pilot.ID).map((lap) => lap.LengthSeconds);
    });
});

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
  fetchRounds(props.eventId).then(() => {
    fetchPilotRaces(props.eventId, props.pilot.ID).then(() => {
      loadingDetails.value = false;
      const raceIds = Object.values(raceSummaries.value).map((summary: any) => summary.Race);
      Promise.all(raceIds.map(raceId => fetchRawRaceDetail(props.eventId, raceId)));
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
  <Page class="bg-ds-base">
    <ActionBar class="bg-ds-base">
      <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
      <Label text="Pilot" class="font-bold text-lg text-ds-ink" />
      <ActionItem text="Refresh" android.systemIcon="ic_menu_refresh" @tap="refreshData" />
    </ActionBar>
    <GridLayout rows="auto, auto, *" class="bg-ds-base">
      <!-- Fixed header section -->
      <GridLayout row="0" columns="auto, *" class="px-5 pt-2 pb-4">
        <Image col="0" v-if="pilot.PhotoURL" :src="pilot.PhotoURL.startsWith('/') ? 'https://fpvtrackside.com' + pilot.PhotoURL : pilot.PhotoURL" class="h-16 w-16 object-cover rounded-2xl mr-3.5" />
        <Image col="0" v-else src="~/assets/pilot.png" class="h-16 w-16 object-cover rounded-2xl mr-3.5" />
        <StackLayout col="1" verticalAlignment="center">
          <Label :text="props.pilot.Name" class="text-ds-ink font-bold text-2xl" />
          <Label v-if="props.pilot.FirstName || props.pilot.LastName"
            :text="props.pilot.FirstName + ' ' + props.pilot.LastName" class="text-ds-muted2 text-sm mt-0.5" />
          <Label v-if="props.pilot.CatchPhrase" :text="props.pilot.CatchPhrase" class="font-mono-ds text-ds-muted text-xs mt-0.5" />
        </StackLayout>
      </GridLayout>

      <!-- Stat tiles -->
      <GridLayout row="1" columns="*, *" class="px-4 pb-4 bg-transparent">
        <StackLayout col="0" class="stat-tile mr-1.5">
          <Label text="BEST LAP" class="stat-tile-label" />
          <Label :text="overallBestLap ?? 'NA'" class="stat-tile-value text-ds-cyan" />
        </StackLayout>
        <StackLayout col="1" class="stat-tile ml-1.5">
          <Label text="POINTS" class="stat-tile-label" />
          <Label :text="totalPoints.toString()" class="stat-tile-value" />
        </StackLayout>
      </GridLayout>

      <!-- Scrollable content -->
      <ScrollView row="2" v-if="!loadingDetails">
        <StackLayout class="px-4 bg-ds-base">
          <StackLayout v-if="allLapsThisEvent.length > 1" class="bg-ds-surface border border-ds-line rounded-2xl p-3.5 mb-3">
            <GridLayout columns="*, auto" class="mb-2.5 bg-transparent">
              <Label col="0" text="EVERY LAP THIS EVENT" class="font-mono-ds text-[10px] tracking-wide text-ds-muted2" />
              <Label col="1" :text="'PB ' + overallBestLap" class="font-mono-ds text-[10px] text-ds-cyan" />
            </GridLayout>
            <LapSparkline :laps="allLapsThisEvent" :height="70" />
          </StackLayout>

          <StackLayout v-for="round in rounds" :key="round.ID" class="p-3.5 mb-2.5 bg-ds-surface border border-ds-line rounded-2xl"
            @tap="$navigateTo(RaceDetails, { props: { race: getRaceById(round.ID), pilots: props.pilots, round: getRoundById(round.ID) } })">
            <!-- Round title and position/points header -->
            <GridLayout columns="auto, *, auto" class="bg-transparent mb-2.5">
              <Label col="0" :text="'R' + round.RoundNumber" class="font-mono-ds text-ds-muted text-sm mr-2.5" />
              <Label col="1" :text="'Round ' + round.RoundNumber" class="text-ds-ink2 font-semibold text-base" />
              <Label v-if="round.EventType !== 'TimeTrial' && raceSummaries[round.ID]" col="2"
                :text="'P' + (raceSummaries[round.ID]?.Position || '-')"
                class="font-mono-ds text-xs rounded-md px-1.5 py-0.5 mr-2"
                :class="raceSummaries[round.ID]?.Position === '1'
                  ? 'text-ds-cyan bg-ds-cyan-surface border border-ds-cyan-border'
                  : 'text-ds-muted border border-ds-line'" />
              <Label v-if="raceSummaries[round.ID]" col="3" :text="formatRaceTime(raceSummaries[round.ID]?.PbLapTime || raceSummaries[round.ID]?.RaceTime || 'NA')"
                class="font-mono-ds text-ds-ink text-base" />
            </GridLayout>

            <!-- No laps placeholder -->
            <StackLayout v-if="raceSummaries[round.ID]?.LapCount === 0" class="p-4 text-center">
              <Label class="fa text-ds-faint text-3xl mb-2 text-center" text="&#xf017;" />
              <Label text="No laps recorded" class="text-ds-muted text-sm text-center mb-1" />
              <Label text="This pilot hasn't completed any valid laps yet" class="text-ds-muted2 text-xs text-center" />
            </StackLayout>
            <GridLayout v-else-if="getLapsForPilot(raceSummaries[round.ID]?.Race, props.pilot.ID).length > 0"
              :rows="`auto${', auto'.repeat(raceSummaries[round.ID]?.LapCount)}`" columns="*, *"
              class="bg-ds-surface2 rounded-xl p-3">
              <!-- Header Row -->
              <Label row="0" col="0" text="LAP" class="font-mono-ds text-ds-muted2 text-[10px] tracking-wide pb-2"
                style="border-bottom-width: 1px; border-bottom-color: #1F242B;" />
              <Label row="0" col="1" text="TIME" class="font-mono-ds text-ds-muted2 text-[10px] tracking-wide pb-2 text-right"
                style="border-bottom-width: 1px; border-bottom-color: #1F242B;" />
              <!-- Data Rows -->
              <template v-for="(lap, index) in getLapsForPilot(raceSummaries[round.ID]?.Race, props.pilot.ID)"
                :key="lap.ID">
                <Label :row="index + 1" col="0" :text="'#' + lap.LapNumber"
                    class="font-mono-ds text-ds-muted py-1 text-xs" />
                <Label :row="index + 1" col="1" :text="lap.LengthSeconds.toFixed(2)"
                    class="font-mono-ds text-ds-ink2 py-1 text-xs text-right" />
              </template>
            </GridLayout>
            <!-- No data placeholder -->
            <StackLayout v-else-if="!(raceSummaries[round.ID]?.LapCount > 0)" class="p-4 text-center">
              <Label class="fa text-ds-faint text-3xl mb-2 text-center" text="&#xf017;" />
              <Label text="No data available" class="text-ds-muted text-sm text-center mb-1" />
              <Label text="Race data is not available for this round" class="text-ds-muted2 text-xs text-center" />
            </StackLayout>
            <!-- Loading placeholder -->
            <StackLayout v-else class="p-4 text-center">
              <Label class="fa text-ds-faint text-3xl mb-2 text-center" text="&#xf110;" />
              <Label text="Loading lap times..." class="text-ds-muted text-sm text-center mb-1" />
              <Label text="Please wait while we fetch the data" class="text-ds-muted2 text-xs text-center" />
            </StackLayout>
          </StackLayout>
        </StackLayout>
      </ScrollView>
      <ActivityIndicator row="2" v-else busy="true" class="h-16 w-16" />
    </GridLayout>
  </Page>
</template>
