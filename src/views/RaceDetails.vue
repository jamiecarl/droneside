<script lang="ts" setup>
import { defineProps, computed, ref, onMounted } from 'nativescript-vue';
import type { PilotType, RawRaceDataType, RaceDetailType, LapType, RoundType } from 'types/events.vue';
import { formatRaceTime } from "../utils/formatRaceTime";
import { Application, GridLayout, StackLayout } from '@nativescript/core';
import { sortResultsByPosition, sortResultByPbTime } from '../utils/sortResults';

const props = defineProps<{ race: RaceDetailType, round: RoundType, pilots: PilotType[] }>();

const loadingRaces = ref(true);

// Compute podium: sort results by numeric position and take top 3.
const podium = computed(() => {
    if (props.round.EventType === 'TimeTrial') {
        return sortResultByPbTime([...props.race.ResultSummaries]).slice(0, 3);
    }
    return sortResultsByPosition([...props.race.ResultSummaries]).slice(0, 3);
});

const sortedResultSummaries = computed(() => {
    return sortResultsByPosition([...props.race.ResultSummaries]);
});

function getPilotPhotoURL(pilotId: string) {
    const pilot = props.pilots.find((p) => p.ID === pilotId);
    return pilot && pilot.PhotoURL ? pilot.PhotoURL : '~/assets/pilot.png';
}

function getPilotName(pilotId: string) {
    const pilot = props.pilots.find((p) => p.ID === pilotId);
    return pilot && pilot.Name ? pilot.Name : 'Unknown Pilot';
}

function getPilotCatchPhrase(pilotId: string) {
    const pilot = props.pilots.find((p) => p.ID === pilotId);
    return pilot && pilot.CatchPhrase ? pilot.CatchPhrase : '';
}

// New raw race detail (includes Laps array)
const rawRace = ref<(RawRaceDataType & { Laps: LapType[] }) | null>(null);

async function fetchRawRaceDetail() {
    try {
        console.log('Fetching raw race detail for race:', props.race.ID);
        const url = `https://fpvtrackside.com/api/public/races/eventId/${props.race.Event}/raceId/${props.race.ID}/raw/1`;
        const response = await fetch(url);
        const data = await response.json();
        rawRace.value = data;
    } catch (error) {
        console.error('Error fetching raw race detail:', error);
    } finally {
        loadingRaces.value = false;
    }
}

async function fetchRoundDetails(eventId: string, roundId: string) {
    try {
        console.log('Fetching event details:', props.race.Event);
        const eventResponse = await fetch(`https://fpvtrackside.com/api/public/events/id/${props.race.Event}`);
        await eventResponse.json();
        console.log('Fetching round details for event:', eventId, 'round:', roundId);
        const response = await fetch(`https://fpvtrackside.com/api/public/races/eventId/${eventId}/roundId/${roundId}`);
        const data = await response.json();
        // Update the current race data with the refreshed data
        const updatedRace = data.find((r: RaceDetailType) => r.ID === props.race.ID);
        if (updatedRace) {
            Object.assign(props.race, updatedRace);
        }
    } catch (error) {
        console.error('Error fetching round details:', error);
    } finally {
        fetchRawRaceDetail();
    }
}

async function refreshData() {
    fetchRoundDetails(props.round.Event, props.round.ID)
}

// Helper: return laps for a given pilot using corresponding detections
function getLapsForPilot(pilotId: string): LapType[] {
    if (!rawRace.value || !rawRace.value.Laps || !rawRace.value.Detections) return [];
    const pilotDetectionIds = rawRace.value.Detections
        .filter(detection => detection.Pilot === pilotId && detection.Valid !== false && detection.LapNumber > 0)
        .map(detection => detection.ID);
    return rawRace.value.Laps
        .filter(lap => pilotDetectionIds.includes(lap.Detection))
        .sort((a, b) => a.LapNumber - b.LapNumber);
}

function getRaceTime(result: any) {
    if (props.round.EventType === 'TimeTrial') {
        return result.PbLapTime || 'DNF';
    }
    return result.RaceTime || 'DNF';
}

// Format date for display
function formatDateTime(dateTimeString: string): string {
    if (!dateTimeString) return 'Not available';
    const date = new Date(dateTimeString);
    return date.getHours() + ':' + date.getMinutes() + ':' + date.getSeconds();
}

onMounted(() => {
    loadingRaces.value = true;
    fetchRawRaceDetail();
    Application.on(Application.resumeEvent, refreshData);
});
</script>

<template>
    <Page>
        <ActionBar>
            <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
            <!-- Updated header label with event and round details -->
            <Label :text="`Round: ${props.round.RoundNumber} Race: ${race.RaceNumber}`" class="font-bold text-lg" />
            <ActionItem text="Refresh" android.systemIcon="ic_menu_refresh" @tap="refreshData" />
        </ActionBar>
        <!-- Parent container with race timing info (row 0), fixed podium (row 1) and scrollable pilots (row 2) -->
        <GridLayout rows="auto, auto, *">
            <!-- Fixed Podium Section -->
            <StackLayout row="0" class="p-3 bg-white">
                <!-- Podium Section -->
                <StackLayout>
                    <Label text="Podium" class="text-black text-center text-xl font-bold mb-2" />
                    <GridLayout columns="*,auto,*" class="bg-transparent" row="0">
                        <StackLayout v-for="(result, idx) in podium" :key="result.ID" :col="idx">
                            <Label :text="result.Position || '?'" class="text-white text-center mt-2 medal"
                                :class="'medal-' + (result.Position || 0)" />
                            <Label :text="getPilotName(result.Pilot)" class="text-black text-center mt-2" />
                            <Label :text="formatRaceTime(getRaceTime(result))" class="text-black text-center mt-1" />
                        </StackLayout>
                    </GridLayout>
                </StackLayout>
            </StackLayout>
            <!-- Race Timing Section -->
            <StackLayout row="1" class="p-2 bg-gray-100">
                <GridLayout columns="auto, *, auto, *" class="bg-transparent">
                    <Label col="0" text="Start:" class="text-gray-600 text-sm font-bold" />
                    <Label col="1" :text="formatDateTime(props.race.Start)" class="text-black text-sm" />
                    <Label col="2" text="End:" class="text-gray-600 text-sm font-bold" />
                    <Label col="3" :text="formatDateTime(props.race.End)" class="text-black text-sm" />
                </GridLayout>
            </StackLayout>
            <!-- Scrollable All Pilots Section -->
            <ScrollView row="2">
                <StackLayout class="p-3 bg-transparent">
                    <!-- Detailed Pilot Overview Stats Section -->
                    <StackLayout v-for="result in sortedResultSummaries" :key="result.ID"
                        class="p-4 my-2 bg-gray-800 rounded-md">
                        <GridLayout columns="auto, *, auto" class="bg-transparent mb-1">
                            <Image col="0" :src="getPilotPhotoURL(result.Pilot)"
                                class="h-16 w-16 object-cover rounded-lg mr-2" />
                            <GridLayout col="1" rows="auto, auto" class="bg-transparent">
                                <Label row="0" :text="getPilotName(result.Pilot)"
                                    class="text-white font-bold text-lg align-top" />
                                <Label row="1" :text="getPilotCatchPhrase(result.Pilot)"
                                    class="text-gray-500 font-bold text-xs  align-top" />
                            </GridLayout>
                            <GridLayout col="2" rows="auto, auto" class="bg-transparent">
                                <Label v-if="props.round.EventType !== 'TimeTrial'" row="0"
                                    :text="result.Position || 'NA'" width="40" height="40"
                                    class="text-xl font-bold text-white text-center bg-white rounded-md"
                                    style="background-color: rgba(255,255,255,0.2);" />
                                <Label v-if="parseInt(result.Points) > 0" row="1" :text="result.Points + ' points'"
                                    class="text-white text-xs" />
                            </GridLayout>
                        </GridLayout>
                        <GridLayout columns="auto, auto" class="bg-transparent mb-5 text-xs">
                            <GridLayout col="0" rows="auto, *, auto" class="bg-transparent">
                                <Label v-if="props.round.EventType !== 'TimeTrial'" row="0" text="Holeshot:"
                                    class="text-gray-400 mr-2" />
                                <Label row="1" text="Best Lap:" class="text-gray-400 mr-2" />
                                <Label v-if="props.round.EventType !== 'TimeTrial'" row="2" text="Race:"
                                    class="text-gray-400 mr-2" />
                            </GridLayout>
                            <GridLayout col="1" rows="auto, *, auto" class="bg-transparent">
                                <Label v-if="props.round.EventType !== 'TimeTrial'" row="0"
                                    :text="formatRaceTime(result.HoleshotTime || 'NA')" class="text-yellow-400 mr-2" />
                                <Label row="1" :text="formatRaceTime(result.PbLapTime || 'NA')"
                                    class="text-green-500 mr-2" />
                                <Label v-if="props.round.EventType !== 'TimeTrial'" row="2"
                                    :text="formatRaceTime(getRaceTime(result))" class="text-white mr-2" />
                            </GridLayout>
                        </GridLayout>
                        <!-- Lap Details Table: now only visible if rawRace exists -->
                        <GridLayout v-if="!loadingRaces"
                            :rows="`auto${', auto'.repeat(getLapsForPilot(result.Pilot).length)}`" columns="*, *"
                            class="bg-transparent">
                            <!-- Header Row -->
                            <Label row="0" col="0" text="Lap" class="text-white font-bold"
                                style="border-bottom-width: 1px; border-bottom-color: rgba(255,255,255,0.3);" />
                            <Label row="0" col="1" text="Time" class="text-white font-bold"
                                style="border-bottom-width: 1px; border-bottom-color: rgba(255,255,255,0.3);" />
                            <!-- Data Rows -->
                            <template v-for="(lap, index) in getLapsForPilot(result.Pilot)" :key="lap.ID">
                                <Label :row="index + 1" col="0" :text="'#' + lap.LapNumber" class="text-white" />
                                <Label :row="index + 1" col="1" :text="lap.LengthSeconds.toFixed(2)"
                                    class="text-white" />
                            </template>
                        </GridLayout>
                        <Label v-else-if="loadingRaces" text="Loading race data..." class="text-white text-center" />
                    </StackLayout>
                </StackLayout>
            </ScrollView>
        </GridLayout>
    </Page>
</template>
