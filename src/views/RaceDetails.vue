<script lang="ts" setup>
import { defineProps, computed, ref, onMounted } from 'nativescript-vue';
import type { PilotType, RaceDetailType, ResultSummaryType, LapType, EventType, RoundType } from 'types/events.vue';

const props = defineProps<{ race: RaceDetailType, round: RoundType, pilots: PilotType[] }>();

// Compute podium: sort results by numeric position and take top 3.
const podium = computed(() => {
    const sorted = [...props.race.ResultSummaries].sort((a, b) => Number(a.Position) - Number(b.Position));
    return sorted.slice(0, 3);
});

function getPilotName(pilotId: string) {
    const pilot = props.pilots.find((p) => p.ID === pilotId);
    return pilot ? pilot.Name : 'Unknown Pilot';
}

function formatRaceTime(time: string): string {
    const num = parseFloat(time);
    return isNaN(num) ? time : num.toFixed(2);
}

// New raw race detail (includes Laps array)
const rawRace = ref<(RaceDetailType & { Laps: LapType[] }) | null>(null);

async function fetchRawRaceDetail() {
    try {
        console.log('Fetching raw race detail for race:', props.race.ID);
        const url = `https://fpvtrackside.com/api/public/races/eventId/${props.race.Event}/raceId/${props.race.ID}/raw/1`;
        const response = await fetch(url);
        const data = await response.json();
        rawRace.value = data;
    } catch (error) {
        console.error('Error fetching raw race detail:', error);
    }
}

// Helper: return laps for a given pilot using corresponding detections
function getLapsForPilot(pilotId: string): LapType[] {
    if (!rawRace.value || !rawRace.value.Laps || !rawRace.value.Detections) return [];
    const pilotDetectionIds = rawRace.value.Detections
        .filter(detection => detection.Pilot === pilotId)
        .map(detection => detection.ID);
    return rawRace.value.Laps
        .filter(lap => pilotDetectionIds.includes(lap.Detection))
        .sort((a, b) => a.LapNumber - b.LapNumber);
}

onMounted(() => {
    fetchRawRaceDetail();
});
</script>

<template>
    <Page>
        <ActionBar>
            <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
            <!-- Updated header label with event and round details -->
            <Label :text="`Round: ${props.round.RoundNumber} Race: ${race.RaceNumber}`" class="font-bold text-lg" />
        </ActionBar>
        <ScrollView>
            <StackLayout class="p-3 bg-black">
                <!-- Podium Section -->
                <StackLayout class="mb-4">
                    <Label text="Podium" class="text-white text-xl font-bold mb-2" />
                    <GridLayout columns="*,*,*" class="px-5" row="0">
                        <StackLayout v-for="(result, idx) in podium" :key="result.ID" class="p-2" :col="idx">
                            <Label :text="result.Position" class="text-white text-center mt-2 medal"
                                :class="'medal-' + result.Position" />
                            <Label :text="getPilotName(result.Pilot)" class="text-white text-center mt-2" />
                            <Label :text="formatRaceTime(result.RaceTime || 'DNF')"
                                class="text-white text-center mt-1" />
                        </StackLayout>
                    </GridLayout>
                </StackLayout>
                <!-- Detailed Pilot Overview Stats Section -->
                <StackLayout>
                    <Label text="All Pilots" class="text-white text-xl font-bold mb-2" />
                    <StackLayout v-for="result in props.race.ResultSummaries" :key="result.ID"
                        class="mb-3 p-2 bg-gray-800 rounded">
                        <Label :text="getPilotName(result.Pilot)" class="text-white font-bold mb-1" />
                        <GridLayout columns="auto, auto, auto, auto, auto, auto" class="mb-1">
                            <Label col="0" :text="'Holeshot: ' + formatRaceTime(result.HoleshotTime)"
                                class="text-white mr-2" />
                            <Label col="1" :text="'PbLap: ' + formatRaceTime(result.PbLapTime)"
                                class="text-white mr-2" />
                            <Label col="2" :text="'PbCount: ' + result.PbLapCount" class="text-white mr-2" />
                            <Label col="3" :text="'Race: ' + formatRaceTime(result.RaceTime)" class="text-white mr-2" />
                            <Label col="4" :text="'Pos: ' + result.Position" class="text-white mr-2" />
                            <Label col="5" :text="'Points: ' + result.Points" class="text-white" />
                        </GridLayout>
                        <!-- Lap Details Table: now only visible if rawRace exists -->
                        <template v-if="rawRace">
                            <GridLayout columns="auto, auto, auto" class="mb-2">
                                <Label col="0" text="Lap" class="text-white font-bold" />
                                <Label col="1" text="Pos" class="text-white font-bold" />
                                <Label col="2" text="Time" class="text-white font-bold" />
                            </GridLayout>
                            <GridLayout v-for="lap in getLapsForPilot(result.Pilot)" :key="lap.ID"
                                columns="auto, auto" class="mb-1">
                                <Label col="0" :text="lap.LapNumber" class="text-white" />
                                <Label col="2" :text="lap.LengthSeconds.toFixed(2)" class="text-white" />
                            </GridLayout>
                        </template>
                    </StackLayout>
                </StackLayout>
            </StackLayout>
        </ScrollView>
    </Page>
</template>
