<script lang="ts" setup>
import { computed } from 'nativescript-vue';
import { GridLayout, StackLayout } from '@nativescript/core';
import { defineProps, ref, onMounted } from 'nativescript-vue';
import type { EventType, PilotType, RoundType } from 'types/events.vue';
import { formatRaceTime } from '~/utils/formatRaceTime';

const props = defineProps<{ pilot: PilotType, event: EventType }>();
const loadingDetails = ref(true);
const rounds = ref<{ [key: string]: RoundType }>({});
const raceSummaries = ref<{ [key: string]: any }>({});

const raceSummariesCount = computed(() => Object.keys(raceSummaries.value).length);

async function fetchRounds(eventId: string) {
  try {
    console.log('Fetching rounds for event:', eventId);
    const response = await fetch(`https://fpvtrackside.com/api/public/rounds/eventId/${eventId}`);
    const data = await response.json();
    const indexedRounds = data.reduce((acc: { [key: string]: RoundType }, round: RoundType) => {
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
    const summaries = data.reduce((acc: { [key: string]: any }, race: any) => {
      const summary = race.ResultSummaries.find((summary: any) => summary.Pilot === pilotId);
      if (summary) {
        acc[race.Round] = summary;
      }
      return acc;
    }, {});
    raceSummaries.value = summaries;
  } catch (error) {
    console.error('Error fetching pilot races:', error);
  }
}

onMounted(() => {
  loadingDetails.value = true;
  fetchRounds(props.event.ID).then(() => {
    fetchPilotRaces(props.event.ID, props.pilot.ID).then(() => {
      loadingDetails.value = false;
    });
  });
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
          <Label text="Race Details" class="text-center text-lg font-bold" />
          <StackLayout>
            <StackLayout v-for="round in rounds" :key="round.ID" class="mb-3 p-4 mb-2 bg-gray-800 rounded-md">
              <GridLayout columns="*, auto" class="bg-transparent">
                <StackLayout>
                  <Label :text="'Round #' + round.RoundNumber" class="text-white text-lg font-bold" />
                  <GridLayout columns="auto, *, auto" class="bg-transparent mb-5 text-xs">
                    <GridLayout col="0" rows="auto, *, auto" class="bg-transparent">
                      <Label v-if="round.EventType !== 'TimeTrial'" row="0" text="Holeshot:"
                        class="text-gray-400 mr-2" />
                      <Label row="1" text="Best Lap:" class="text-gray-400 mr-2" />
                      <Label v-if="round.EventType !== 'TimeTrial'" row="2" text="Race:" class="text-gray-400 mr-2" />
                    </GridLayout>
                    <GridLayout col="1" rows="auto, *, auto" class="bg-transparent">
                      <Label v-if="round.EventType !== 'TimeTrial'" row="0"
                        :text="formatRaceTime(raceSummaries[round.ID]?.HoleshotTime || 'NA')"
                        class="text-yellow-400 mr-2" />
                      <Label row="1" :text="formatRaceTime(raceSummaries[round.ID]?.PbLapTime || 'NA')"
                        class="text-green-500 mr-2" />
                      <Label v-if="round.EventType !== 'TimeTrial'" row="2"
                        :text="formatRaceTime(raceSummaries[round.ID]?.RaceTime || 'DNF')" class="text-white mr-2" />
                    </GridLayout>
                  </GridLayout>
                </StackLayout>
                <GridLayout col="2" rows="auto, auto" class="bg-transparent">
                  <Label v-if="round.EventType !== 'TimeTrial'" row="0" width="40" height="40"
                    :text="raceSummaries[round.ID]?.Position || 'NA'"
                    class="text-xl font-bold text-black text-center rounded-md"
                    :class="'medal-' + (raceSummaries[round.ID]?.Position || '8')" />
                  <Label v-if="parseInt(raceSummaries[round.ID]?.Points) > 0" row="1"
                    :text="raceSummaries[round.ID]?.Points + ' points'" class="text-white text-xs" />
                </GridLayout>
              </GridLayout>
            </StackLayout>
          </StackLayout>
        </StackLayout>
      </ScrollView>
    </GridLayout>
  </Page>
</template>
