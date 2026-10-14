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

function getRaceStatus(race: RaceDetailType): 'COMPLETE' | 'RUNNING' | 'NOT STARTED' {
    if (race.End && race.Start && race.End > race.Start) {
        return 'COMPLETE';
    }
    if (race.Start && race.Start !== '' && race.Start > '0001-01-01 00:00:00') {
        return 'RUNNING';
    }
    return 'NOT STARTED';
}

onMounted(() => {
    fetchRoundDetails(props.round.Event, props.round.ID);
});
</script>

<template>
    <Page class="bg-ds-base">
        <ActionBar class="bg-ds-base">
            <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
            <Label :text="'Round ' + round.RoundNumber" class="font-bold text-lg text-ds-ink" />
            <ActionItem text="Refresh" android.systemIcon="ic_menu_refresh"
                @tap="fetchRoundDetails(props.round.Event, props.round.ID)" />
        </ActionBar>
        <!-- Wrap content in a GridLayout with fixed header -->
        <GridLayout rows="auto, *" class="bg-ds-base">
            <!-- Fixed header section -->
            <StackLayout row="0" class="px-5 pt-1 pb-4">
                <Label :text="props.event.Name" class="text-ds-muted2 text-sm mb-1" textWrap="true" />
                <GridLayout columns="*, auto">
                    <Label col="0" :text="'Round ' + round.RoundNumber" class="text-2xl font-bold text-ds-ink" />
                    <Label col="1" :text="round.EventType"
                        class="font-mono-ds text-[10px] tracking-wide uppercase text-ds-cyan bg-ds-cyan-surface border border-ds-cyan-border rounded-md px-2 py-1" />
                </GridLayout>
            </StackLayout>
            <!-- Scrollable content -->
            <ScrollView row="1" v-if="!loadingDetails && raceDetails.length > 0">
                <StackLayout class="px-4 bg-ds-base">
                    <StackLayout v-for="(race, index) in raceDetails" :key="race.ID"
                        class="p-4 mb-2.5 bg-ds-surface border border-ds-line rounded-2xl"
                        @tap="$navigateTo(RaceDetails, { props: { race, pilots: props.pilots, round: props.round } })">
                        <GridLayout columns="*, auto" class="mb-2.5 bg-transparent">
                            <Label :text="'Race ' + race.RaceNumber" class="text-ds-ink text-base font-semibold" />
                            <Label col="1" :text="getRaceStatus(race) + (props.round.EventType !== 'TimeTrial' ? ' · ' + race.TargetLaps + ' LAPS' : '')"
                                class="font-mono-ds text-[10px] tracking-wide text-ds-muted text-right"
                                :class="getRaceStatus(race) === 'RUNNING' ? 'text-ds-cyan' : 'text-ds-muted'" />
                        </GridLayout>
                        <GridLayout v-for="result in race.ResultSummaries" :key="result.ID"
                            columns="14, auto, *, auto" class="mb-1.5 bg-transparent">
                            <Label col="0" :text="result.Position || '-'" class="font-mono-ds text-xs text-ds-muted text-center" />
                            <Label col="1" width="3" height="16" class="mr-2.5 rounded-sm channel"
                                :style="{ backgroundColor: result?.Channel?.Color || '#3B424B' }" />
                            <Label col="2" :text="result.PilotName" class="text-sm text-ds-ink2" />
                            <Label col="3" :text="formatRaceTime(getRaceTime(result))" class="font-mono-ds text-sm text-ds-ink text-right" />
                        </GridLayout>
                    </StackLayout>
                </StackLayout>
            </ScrollView>

            <!-- No races placeholder -->
            <StackLayout row="1" v-else-if="!loadingDetails && raceDetails.length === 0" class="p-8 bg-ds-base">
                <StackLayout class="text-center">
                    <Label class="fa text-ds-faint text-5xl mb-4 text-center" text="&#xf11e;" />
                    <Label text="No races found" class="text-ds-ink text-lg font-semibold text-center mb-2" />
                    <Label text="This round doesn't have any races" class="text-ds-muted2 text-base text-center" />
                </StackLayout>
            </StackLayout>

            <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
        </GridLayout>
    </Page>
</template>
