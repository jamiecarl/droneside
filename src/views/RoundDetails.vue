<script lang="ts" setup>
import { GridLayout } from '@nativescript/core';
import { defineProps, ref, onMounted, computed } from 'nativescript-vue';
import { EventType, RoundType, RaceDetailType, PilotType, ChannelType } from 'types/events.vue';
import RaceDetails from './RaceDetails.vue';
import { formatRaceTime } from "../utils/formatRaceTime";
import { sortResultByPbTime, sortResultsByPosition } from '../utils/sortResults';

const props = defineProps<{ event: EventType; round: RoundType; pilots: PilotType[]; channels: ChannelType[] }>();

const raceDetails = ref<RaceDetailType[]>([]);
const loadingDetails = ref(true);

async function fetchRoundDetails(eventId: string, roundId: string) {
    try {
        console.log('Fetching round details for event:', eventId, 'round:', roundId);
        const response = await fetch(`https://fpvtrackside.com/api/public/races/eventId/${eventId}/roundId/${roundId}`);
        const data = await response.json();
        // Sort races by RaceNumber
        data.sort((a: RaceDetailType, b: RaceDetailType) => a.RaceNumber - b.RaceNumber);
        // Sort each race's ResultSummaries by Position numerically
        data.forEach((race: RaceDetailType) => {
            race.ResultSummaries = props.round.EventType === 'TimeTrial'
                ? sortResultByPbTime(race.ResultSummaries)
                : sortResultsByPosition(race.ResultSummaries);
        });
        raceDetails.value = data;
    } catch (error) {
        console.error('Error fetching round details:', error);
    } finally {
        loadingDetails.value = false;
    }
}

function getPilotName(pilotId: string) {
    const pilot = props.pilots.find((p) => p.ID === pilotId);
    return pilot ? pilot.Name : 'Unknown Pilot';
}

/* Return a CSS class from channels based on the result index.
   Cycles through channels. Assumes channel.Name is of the form "B{band}{channelNumber}" */
function getPilotChannelClass(pilotId: string): string {
    return 'channel-' + getPilotChannel(pilotId);
}

function getPilotChannel(pilotId: string): string {
    if (props.channels.length === 0) return 'NA';
    const race = raceDetails.value.find(r => r.PilotChannels.some(pc => pc.Pilot === pilotId));
    if (race) {
        const pilotChannel = race.PilotChannels.find(pc => pc.Pilot === pilotId);
        if (pilotChannel) {
            const channel = props.channels.find(c => c.ID === pilotChannel.Channel);
            if (channel) {
                return channel.ShortBand + channel.Number;
            }
        }
    }
    return 'R1';
}

function getRaceTime(result: any) {
    if (props.round.EventType === 'TimeTrial') {
        return result.PbLapTime || 'DNF';
    }
    return result.RaceTime || 'DNF';
}

onMounted(() => {
    fetchRoundDetails(props.round.Event, props.round.ID);
});
</script>

<template>
    <Page>
        <ActionBar>
            <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
            <Label :text="'Round ' + round.RoundNumber + ' Details'" class="font-bold text-lg" />
        </ActionBar>
        <!-- Wrap content in a GridLayout with fixed header -->
        <GridLayout rows="auto, *">
            <!-- Fixed header section -->
            <StackLayout row="0" class="p-3 bg-white">
                <Label :text="props.event.Name" class="text-black font-bold text-lg" />
                <Label :text="'Round #' + round.RoundNumber" class="text-black font-bold text-lg" />
                <Label :text="round.EventType" class="text-black text-base" />
            </StackLayout>
            <!-- Scrollable content -->
            <ScrollView row="1" v-if="!loadingDetails">
                <StackLayout class="p-3 bg-black">
                    <StackLayout v-for="(race, index) in raceDetails" :key="race.ID"
                        class="p-4 my-2 bg-gray-800 rounded-md"
                        @tap="$navigateTo(RaceDetails, { props: { race, pilots: props.pilots, round: props.round } })">
                        <GridLayout columns="*, auto" class="mb-2 bg-transparent">
                            <Label :text="'Race #' + race.RaceNumber" class="text-white text-lg font-bold" />
                            <Label col="1" :text="'Laps: ' + race.TargetLaps"
                                class="text-white text-lg font-bold text-right" />
                        </GridLayout>
                        <GridLayout v-for="result in race.ResultSummaries" :key="result.ID"
                            columns="24, auto, *, auto, auto" class="ml-4 mb-1 bg-transparent">
                            <Label col="0" :text="result.Position || '-'" class="text-white text-center mr-1" />
                            <Label col="1" :text="getPilotChannel(result.Pilot)" width="16" height="16" class="mr-3"
                                :class="'channel ' + getPilotChannelClass(result.Pilot)" />
                            <Label col="2" :text="getPilotName(result.Pilot)" class="text-white" />
                            <Label col="3" :text="formatRaceTime(getRaceTime(result))"
                                class="text-white text-right" />
                        </GridLayout>
                    </StackLayout>
                </StackLayout>
            </ScrollView>
        </GridLayout>
    </Page>
</template>
