<script lang="ts" setup>
import { defineProps, computed, ref, onMounted, $navigateTo } from 'nativescript-vue';
import type { PilotType, RawRaceDataType, RaceDetailType, LapType, RoundType } from 'types/events.vue';
import { formatRaceTime } from "../utils/formatRaceTime";
import { Application, GridLayout, StackLayout } from '@nativescript/core';
import { sortResultsByPosition, sortResultByPbTime } from '../utils/sortResults';
import PilotDetails from './PilotDetails.vue';
import LapSparkline from '../components/LapSparkline.vue';

const props = defineProps<{ race: RaceDetailType, round: RoundType, pilots: PilotType[] }>();

const loadingRaces = ref(true);

// Compute podium: sort results by numeric position and take top 3, but only if race is finished.
const podium = computed(() => {
    const isRaceFinished = props.race.End && props.race.Start && props.race.End > props.race.Start;
    if (!isRaceFinished) {
        return null;
    }
    
    if (props.round.EventType === 'TimeTrial') {
        return sortResultByPbTime([...props.race.ResultSummaries]).slice(0, 3);
    }
    return sortResultsByPosition([...props.race.ResultSummaries]).slice(0, 3);
});

// Display order for the podium blocks: 2nd, 1st, 3rd (tallest in the middle), matching the redesign.
const orderedPodium = computed(() => {
    if (!podium.value) return null;
    if (podium.value.length === 3) {
        return [podium.value[1], podium.value[0], podium.value[2]];
    }
    return podium.value;
});

function podiumBlockHeight(result: any): string {
    const position = parseInt(result.Position) || 3;
    if (position === 1) return '92px';
    if (position === 2) return '62px';
    return '46px';
}

// Check if race has started (has a valid start time)
const hasRaceStarted = computed(() => {
    return props.race.Start && props.race.Start !== '' && props.race.Start > '0001-01-01 00:00:00';
});

// Calculate race duration if both start and end times are available
const raceDuration = computed(() => {
    if (!props.race.Start || !props.race.End || props.race.End <= props.race.Start) {
        return null;
    }
    
    const startTime = new Date(props.race.Start);
    const endTime = new Date(props.race.End);
    const durationMs = endTime.getTime() - startTime.getTime();
    
    // Convert to minutes and seconds
    const totalSeconds = Math.floor(durationMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
});

const sortedResultSummaries = computed(() => {
    return sortResultsByPosition([...props.race.ResultSummaries]);
});

function getPilotPhotoURL(pilotId: string) {
    const pilot = props.pilots.find((p) => p.ID === pilotId);
    if (!pilot || !pilot.PhotoURL) {
        return '~/assets/pilot.png';
    }
    
    // If PhotoURL is a relative path, prepend the base URL
    if (pilot.PhotoURL.startsWith('/')) {
        return 'https://fpvtrackside.com' + pilot.PhotoURL;
    }
    
    return pilot.PhotoURL;
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

function getAvgLapForPilot(pilotId: string): string {
    const laps = getLapsForPilot(pilotId);
    if (laps.length === 0) return 'NA';
    const total = laps.reduce((sum, lap) => sum + lap.LengthSeconds, 0);
    return (total / laps.length).toFixed(2);
}

function getRaceTime(result: any) {
    const placeholder = result.Position ? 'DNF' : '-';
    if (props.round.EventType === 'TimeTrial') {
        return result.PbLapTime || placeholder;
    }
    return result.RaceTime || placeholder;
}

// Format date for display
function formatDateTime(dateTimeString: string): string {
    if (!dateTimeString) return 'Not available';
    const date = new Date(dateTimeString);
    return date.getHours() + ':' + date.getMinutes() + ':' + date.getSeconds();
}

// Navigate to pilot details
function goToPilotDetails(pilotId: string) {
    const pilot = props.pilots.find(p => p.ID === pilotId);
    if (pilot) {
        $navigateTo(PilotDetails, { 
            props: { 
                pilot, 
                eventId: props.round.Event,
                pilots: props.pilots 
            } 
        });
    }
}

onMounted(() => {
    loadingRaces.value = true;
    fetchRawRaceDetail();
    Application.on(Application.resumeEvent, refreshData);
});
</script>

<template>
    <Page class="bg-ds-base">
        <ActionBar class="bg-ds-base">
            <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
            <!-- Updated header label with event and round details -->
            <Label :text="`Round ${props.round.RoundNumber} · Race ${race.RaceNumber}`" class="font-bold text-lg text-ds-ink" />
            <ActionItem text="Refresh" android.systemIcon="ic_menu_refresh" @tap="refreshData" />
        </ActionBar>
        <!-- Parent container with conditional sections based on race state -->
        <GridLayout :rows="podium && hasRaceStarted ? 'auto, auto, *' : hasRaceStarted ? 'auto, *' : '*'" class="bg-ds-base">
            <!-- Fixed Podium Section - only show if race is finished -->
            <StackLayout v-if="podium" row="0" class="px-5 pt-2 pb-3">
                <!-- Podium Section -->
                <GridLayout columns="*,*,*" class="bg-transparent">
                    <StackLayout v-for="(result, idx) in orderedPodium" :key="result.ID" :col="idx" verticalAlignment="bottom">
                        <Image :src="getPilotPhotoURL(result.Pilot)"
                            :class="result.Position === '1' ? 'h-10 w-10' : 'h-8 w-8'"
                            class="object-cover rounded-xl self-center mb-2" />
                        <StackLayout :class="result.Position === '1'
                            ? 'bg-ds-cyan-surface border border-ds-cyan-border'
                            : 'bg-ds-surface2 border border-ds-line'"
                            class="rounded-t-xl items-center py-2" :style="{ height: podiumBlockHeight(result) }">
                            <Label :text="result.Position || '?'" class="font-mono-ds font-semibold"
                                :class="result.Position === '1' ? 'text-xl text-ds-cyan' : 'text-base text-ds-muted'" />
                            <Label :text="getPilotName(result.Pilot)" class="text-xs mt-0.5"
                                :class="result.Position === '1' ? 'text-ds-ink font-semibold' : 'text-ds-muted2'" textWrap="false" />
                            <Label :text="formatRaceTime(getRaceTime(result))" class="font-mono-ds text-[11px] mt-0.5"
                                :class="result.Position === '1' ? 'text-ds-cyan' : 'text-ds-muted'" />
                        </StackLayout>
                    </StackLayout>
                </GridLayout>
            </StackLayout>
            <!-- Race Timing Section - only show if race has started -->
            <StackLayout v-if="hasRaceStarted" :row="podium ? 1 : 0" class="px-5 py-2.5 border-t border-b border-ds-line-soft">
                <GridLayout :columns="raceDuration ? 'auto, *, auto, 2*, auto' : 'auto, *, auto, *'" class="bg-transparent">
                    <Label col="0" text="START" class="font-mono-ds text-[10px] tracking-wide text-ds-muted2 mr-1.5" />
                    <Label col="1" :text="formatDateTime(props.race.Start)" class="font-mono-ds text-xs text-ds-muted" />
                    <Label col="2" text="END" class="font-mono-ds text-[10px] tracking-wide text-ds-muted2 mr-1.5" />
                    <Label col="3" :text="formatDateTime(props.race.End)" class="font-mono-ds text-xs text-ds-muted" />
                    <Label v-if="raceDuration" col="4" :text="raceDuration" class="font-mono-ds text-xs text-ds-muted text-right" />
                </GridLayout>
            </StackLayout>
            <!-- Scrollable All Pilots Section -->
            <ScrollView :row="podium && hasRaceStarted ? 2 : hasRaceStarted ? 1 : 0">
                <StackLayout class="p-4 bg-transparent">
                    <!-- Detailed Pilot Overview Stats Section -->
                    <StackLayout v-for="result in sortedResultSummaries" :key="result.ID"
                        class="p-3.5 mb-2.5 bg-ds-surface border border-ds-line rounded-2xl">
                        <GridLayout columns="auto, *, auto" class="bg-transparent mb-3">
                            <Image col="0" :src="getPilotPhotoURL(result.Pilot)"
                                class="h-11 w-11 object-cover rounded-xl mr-3"
                                @tap="goToPilotDetails(result.Pilot)" />
                            <GridLayout col="1" rows="auto, auto" class="bg-transparent"
                                @tap="goToPilotDetails(result.Pilot)">
                                <Label row="0" :text="getPilotName(result.Pilot)"
                                    class="text-ds-ink font-semibold text-base align-top" />
                                <Label row="1" :text="getLapsForPilot(result.Pilot).length + ' laps'"
                                    class="font-mono-ds text-ds-muted2 text-[10px] align-top tracking-wide" />
                            </GridLayout>
                            <StackLayout col="2">
                                <Label v-if="props.round.EventType !== 'TimeTrial'"
                                    :text="result.Position || 'NA'"
                                    class="font-mono-ds text-xl font-semibold text-right"
                                    :class="result.Position === '1' ? 'text-ds-cyan' : 'text-ds-ink'" />
                                <Label v-if="parseInt(result.Points) > 0" :text="result.Points + ' PTS'"
                                    class="font-mono-ds text-ds-muted2 text-[10px] text-right" />
                            </StackLayout>
                        </GridLayout>
                        <LapSparkline v-if="!loadingRaces && getLapsForPilot(result.Pilot).length > 1"
                            :laps="getLapsForPilot(result.Pilot).map(l => l.LengthSeconds)" :height="46" class="mb-2.5" />
                        <!-- Race timing display - single row layout -->
                        <GridLayout v-if="props.round.EventType !== 'TimeTrial'" columns="*, *, *" class="bg-transparent">
                            <StackLayout col="0" class="stat-tile mr-1.5">
                                <Label text="HOLESHOT" class="stat-tile-label" />
                                <Label :text="formatRaceTime(result.HoleshotTime || 'NA')" class="stat-tile-value text-ds-holeshot" />
                            </StackLayout>
                            <StackLayout col="1" class="stat-tile mx-1.5">
                                <Label text="BEST LAP" class="stat-tile-label" />
                                <Label :text="formatRaceTime(result.PbLapTime || 'NA')" class="stat-tile-value text-ds-cyan" />
                            </StackLayout>
                            <StackLayout col="2" class="stat-tile ml-1.5">
                                <Label text="AVG" class="stat-tile-label" />
                                <Label :text="getAvgLapForPilot(result.Pilot)" class="stat-tile-value" />
                            </StackLayout>
                        </GridLayout>
                        <!-- TimeTrial layout - only shows Best -->
                        <StackLayout v-else class="stat-tile">
                            <Label text="BEST LAP" class="stat-tile-label" />
                            <Label :text="formatRaceTime(result.PbLapTime || 'NA')" class="stat-tile-value text-ds-cyan" />
                        </StackLayout>
                        <!-- Lap Details Table: now only visible if rawRace exists -->
                        <GridLayout v-if="!loadingRaces && getLapsForPilot(result.Pilot).length > 0"
                            :rows="`auto${', auto'.repeat(getLapsForPilot(result.Pilot).length)}`" columns="*, *"
                            class="bg-ds-surface2 rounded-xl p-3 mt-2.5">
                            <!-- Header Row -->
                            <Label row="0" col="0" text="LAP" class="font-mono-ds text-ds-muted2 text-[10px] tracking-wide pb-2"
                                style="border-bottom-width: 1px; border-bottom-color: #1F242B;" />
                            <Label row="0" col="1" text="TIME" class="font-mono-ds text-ds-muted2 text-[10px] tracking-wide pb-2 text-right"
                                style="border-bottom-width: 1px; border-bottom-color: #1F242B;" />
                            <!-- Data Rows -->
                            <template v-for="(lap, index) in getLapsForPilot(result.Pilot)" :key="lap.ID">
                                <Label :row="index + 1" col="0" :text="'#' + lap.LapNumber"
                                    class="font-mono-ds text-ds-muted py-1 text-xs" />
                                <Label :row="index + 1" col="1" :text="lap.LengthSeconds.toFixed(2)"
                                    class="font-mono-ds text-ds-ink2 py-1 text-xs text-right" />
                            </template>
                        </GridLayout>

                        <!-- No laps placeholder -->
                        <StackLayout v-else-if="!loadingRaces && getLapsForPilot(result.Pilot).length === 0" class="p-4 text-center">
                            <Label class="fa text-ds-faint text-3xl mb-2 text-center" text="&#xf017;" />
                            <Label text="No laps recorded" class="text-ds-muted text-sm text-center mb-1" />
                            <Label text="This pilot hasn't completed any valid laps" class="text-ds-muted2 text-xs text-center" />
                        </StackLayout>

                        <!-- Loading placeholder -->
                        <StackLayout v-else-if="loadingRaces" class="p-4 text-center">
                            <Label class="fa text-ds-faint text-3xl mb-2 text-center" text="&#xf110;" />
                            <Label text="Loading race data..." class="text-ds-muted text-sm text-center mb-1" />
                            <Label text="Please wait while we fetch the lap times" class="text-ds-muted2 text-xs text-center" />
                        </StackLayout>
                    </StackLayout>
                </StackLayout>
            </ScrollView>
        </GridLayout>
    </Page>
</template>
