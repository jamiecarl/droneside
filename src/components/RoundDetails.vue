<script lang="ts" setup>
import { GridLayout } from '@nativescript/core';
import { defineProps, ref, onMounted } from 'nativescript-vue';
import { RoundType, RaceDetailType } from 'types/events.vue';

const props = defineProps<{ round: RoundType }>();

const raceDetails = ref<RaceDetailType[]>([]);
const loadingDetails = ref(true);

async function fetchRoundDetails(eventId: string, roundId: string) {
    try {
        console.log('Fetching round details for event:', eventId, 'round:', roundId);
        const response = await fetch(`https://fpvtrackside.com/api/public/races/eventId/${eventId}/roundId/${roundId}`);
        const data = await response.json();
        raceDetails.value = data.sort((a: RaceDetailType, b: RaceDetailType) => a.RaceNumber - b.RaceNumber);
    } catch (error) {
        console.error('Error fetching round details:', error);
    } finally {
        loadingDetails.value = false;
    }
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
        <StackLayout>
            <Label v-if="loadingDetails" text="Loading round details..." class="text-center" />
            <GridLayout v-else>
                <Label text="Pilots" class="font-bold text-lg" />
                <ListView :items="raceDetails">
                    <template #default="{ item }">
                        <Label :text="'Race #' + item.RaceNumber" class="text-white" />
                    </template>
                </ListView>
            </GridLayout>
        </StackLayout>
    </Page>
</template>
