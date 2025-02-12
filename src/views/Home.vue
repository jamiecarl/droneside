<script lang="ts" setup>
import {
  ref,
  onMounted,
  $navigateTo,
} from 'nativescript-vue';
import ClubEventHeader from '~/components/ClubEventHeader.vue';
import EventDetails from '~/views/EventDetails.vue';
import { EventType, ChannelType } from 'types/events.vue';

const events = ref<EventType[]>([]);
const channels = ref<ChannelType[]>([]);
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

async function fetchChannels() {
  try {
    console.log('Fetching channels...');
    const response = await fetch('https://fpvtrackside.com/api/public/channels');
    const data = await response.json();
    channels.value = data;
  } catch (error) {
    console.error('Error fetching channels:', error);
  }
}

function formatDate(dateString: string): string {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
}

onMounted(() => {
  console.log('Home mounted');
  fetchEvents();
  fetchChannels();
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
            <ClubEventHeader row="0" :event="event" :formatDate="formatDate"
              @tap="$navigateTo(EventDetails, { props: { event, channels } })" />
          </template>
        </ListView>
        <Label v-else text="Loading events..." class="text-center text-gray-500" />
      </GridLayout>
    </Page>
  </Frame>
</template>
