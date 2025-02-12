<script lang="ts" setup>
import { GridLayout } from '@nativescript/core';
import { defineProps, ref, onMounted } from 'nativescript-vue';
import { RoundType, RaceDetailType, PilotType, ChannelType } from 'types/events.vue';

const props = defineProps<{ round: RoundType; pilots: PilotType[]; channels: ChannelType[] }>();

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
            race.ResultSummaries.sort((a, b) => Number(a.Position) - Number(b.Position));
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

function formatRaceTime(time: string): string {
    const num = parseFloat(time);
    return isNaN(num) ? time : num.toFixed(2);
}

onMounted(() => {
    fetchRoundDetails(props.round.Event, props.round.ID);
});
</script>

<template>
    <Page>
        <ActionBar>
            <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
            <Label :text="'Round #' + round.RoundNumber + ' Details'" class="font-bold text-lg" />
        </ActionBar>
        <ScrollView v-if="!loadingDetails">
            <StackLayout class="p-3 bg-black">
                <StackLayout v-for="(race, index) in raceDetails" :key="race.ID"
                    class="p-4 my-2 bg-gray-800 rounded-md">
                    <!-- Changed race header to use grid layout for race number and target laps -->
                    <GridLayout columns="*, auto" class="mb-2 bg-transparent">
                        <Label :text="'Race #' + race.RaceNumber" class="text-white text-lg font-bold" />
                        <Label col="1" :text="'Laps: ' + race.TargetLaps"
                            class="text-white text-lg font-bold text-right" />
                    </GridLayout>
                    <!-- Updated race results grid -->
                    <GridLayout v-for="result in race.ResultSummaries" :key="result.ID" columns="auto, auto, *, auto, auto"
                        class="ml-4 mb-1 bg-transparent">
                        <Label col="0" :text="result.Position" class="text-white mr-1" />
                        <!-- New red box column with dynamic CSS class -->
                        <Label col="1" :text="getPilotChannel(result.Pilot)" width="16" height="16" class="mr-3"
                            :class="'channel ' + getPilotChannelClass(result.Pilot)" />
                        <Label col="2" :text="getPilotName(result.Pilot)" class="text-white" />
                        <Label col="3" :text="result.RaceTime ? formatRaceTime(result.RaceTime) : 'DNF'" class="text-white text-right" />
                    </GridLayout>
                </StackLayout>
            </StackLayout>
        </ScrollView>
    </Page>
</template>
