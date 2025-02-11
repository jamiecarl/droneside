<script lang="ts" setup>
import {
  ref,
  onMounted,
  $navigateTo,
} from 'nativescript-vue';
import EventDetails from './EventDetails.vue';
import { EventType } from 'types/events.vue';

const events = ref<EventType[]>([]);
const loading = ref(true);

async function fetchEvents() {
  try {
    console.log('Fetching events...');
    const response = await fetch('https://fpvtrackside.com/api/public/events');
    const data = await response.json();
    events.value = data.map((event: EventType) => ({
      ...event,
      Club: {
        ...event.Club,
        LogoUrl: `https://fpvtrackside.com${event.Club.LogoUrl}`
      }
    })).sort((a: EventType, b: EventType) => new Date(b.Start).getTime() - new Date(a.Start).getTime());
  } catch (error) {
    console.error('Error fetching events:', error);
  } finally {
    loading.value = false;
  }
}

function formatDate(dateString: string): string {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
}

onMounted(() => {
  console.log('Home mounted');
  fetchEvents();
});
</script>

<template>
  <Frame>
    <Page>
      <ActionBar>
        <Label text="DroneSide" class="font-bold text-lg" />
      </ActionBar>

      <GridLayout class="text-white">
        <ListView v-if="!loading" :items="events">
          <template #default="{ item: event }" class="mb-2">
            <GridLayout columns="auto, *" class="p-4 mb-2 bg-white border-2 border-gray-300"
              @tap="$navigateTo(EventDetails, { props: { event } })">
              <Image col="0" :src="event.Club.LogoUrl" class="h-20 w-20 object-cover rounded-lg mr-2" />
              <StackLayout col="1">
                <Label :text="event.Name" class="text-lg font-bold text-black" />
                <Label :text="event.Club.Name" class="text-sm text-gray-500" />
                <Label :text="formatDate(event.Start)" class="text-sm text-gray-500" />
                <Label :text="'Pilots: ' + event.PilotCount" class="text-sm text-gray-500" />
              </StackLayout>
            </GridLayout>
          </template>
        </ListView>
        <Label v-else text="Loading events..." class="text-center text-gray-500" />
      </GridLayout>
    </Page>
  </Frame>
</template>
