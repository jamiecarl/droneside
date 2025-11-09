<script lang="ts" setup>
import { GridLayout } from '@nativescript/core';
import { defineProps, ref, onMounted, computed } from 'nativescript-vue';
import { EventType, RoundType, RaceDetailType, PilotType, ChannelType, ResultSummaryType } from 'types/events.vue';
import RaceDetails from './RaceDetails.vue';
import { formatRaceTime } from "../utils/formatRaceTime";
import { sortResultByPbTime, sortResultsByPosition } from '../utils/sortResults';

const props = defineProps<{ event: EventType; round: RoundType; pilots: PilotType[]; channels: ChannelType[] }>();

const raceDetails = ref<RaceDetailType[]>([]);
const loadingDetails = ref(true);

async function fetchRoundDetails(eventId: string, roundId: string) {
    try {
        console.log('Fetching event details:', eventId);
        const eventResponse = await fetch(`https://fpvtrackside.com/api/public/events/id/${eventId}`);
        await eventResponse.json();
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
            race.ResultSummaries.forEach((result: ResultSummaryType) => {
                const pilot = props.pilots.find((p) => p.ID === result.Pilot);
                result.PilotName = pilot ? pilot.Name : 'Unknown Pilot';
                const raceChannel = race.PilotChannels.find(c => c.Pilot === result.Pilot);
                if (!raceChannel) {
                    return;
                }
                const pilotChannel = props.channels.find(c => c.ID === raceChannel.Channel);
                if (!pilotChannel) {
                    return;
                }
                result.Channel = pilotChannel;
            });
        });
        raceDetails.value = data;
    } catch (error) {
        console.error('Error fetching round details:', error);
    } finally {
        loadingDetails.value = false;
    }
}

function getRaceTime(result: any) {
    const placeholder = result.Position ? 'DNF' : '-';
    if (props.round.EventType === 'TimeTrial') {
        return result.PbLapTime || placeholder;
    }
    return result.RaceTime || placeholder;
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
            <ActionItem text="Refresh" android.systemIcon="ic_menu_refresh"
                @tap="fetchRoundDetails(props.round.Event, props.round.ID)" />
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
            <ScrollView row="1" v-if="!loadingDetails && raceDetails.length > 0">
                <StackLayout class="p-3 bg-black">
                    <StackLayout v-for="(race, index) in raceDetails" :key="race.ID"
                        class="p-4 my-2 bg-gray-800 rounded-md"
                        @tap="$navigateTo(RaceDetails, { props: { race, pilots: props.pilots, round: props.round } })">
                        <GridLayout columns="*, auto" class="mb-2 bg-transparent">
                            <Label :text="'Race #' + race.RaceNumber" class="text-white text-lg font-bold" />
                            <Label v-if="props.round.EventType !== 'TimeTrial'" col="1" :text="'Laps: ' + race.TargetLaps"
                                class="text-white text-lg font-bold text-right" />
                        </GridLayout>
                        <GridLayout v-for="result in race.ResultSummaries" :key="result.ID"
                            columns="24, auto, *, auto, auto" class="ml-4 mb-1 bg-transparent">
                            <Label col="0" :text="result.Position || '-'" class="text-white text-center mr-1" />
                            <Label col="1" :text="result?.Channel?.DisplayName" width="16" height="16"
                                class="mr-3 channel" :style="{ backgroundColor: result?.Channel?.Color }" />
                            <Label col="2" :text="result.PilotName" class="text-white" />
                            <Label col="3" :text="formatRaceTime(getRaceTime(result))" class="text-white text-right" />
                        </GridLayout>
                    </StackLayout>
                </StackLayout>
            </ScrollView>
            
            <!-- No races placeholder -->
            <StackLayout row="1" v-else-if="!loadingDetails && raceDetails.length === 0" class="p-8 bg-black">
                <StackLayout class="text-center">
                    <Label class="fa text-gray-400 text-6xl mb-4 text-center" text="&#xf11e;" />
                    <Label text="No races found" class="text-white text-xl font-semibold text-center mb-2" />
                    <Label text="This round doesn't have any races" class="text-gray-400 text-base text-center" />
                </StackLayout>
            </StackLayout>
            
            <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
        </GridLayout>
    </Page>
</template>
