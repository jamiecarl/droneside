<script lang="ts" setup>
import {
  ref,
  onMounted,
  computed,
  $navigateTo,
} from 'nativescript-vue';
import ClubEventHeader from '~/components/ClubEventHeader.vue';
import EventDetails from '~/views/EventDetails.vue';
import { EventType, ChannelType } from 'types/events.vue';

const events = ref<EventType[]>([]);
const channels = ref<ChannelType[]>([]);
const loading = ref(true);
const refreshing = ref(false);
const selectedEventFilter = ref<'upcoming' | 'recent' | 'previous'>('upcoming');

// Date helpers
const now = new Date();
const thirtyDaysAgo = new Date(now.getTime() - (30 * 24 * 60 * 60 * 1000));

// Computed filtered events
const filteredEvents = computed(() => {
  return events.value.filter(event => {
    const eventStart = new Date(event.Start);
    const eventEnd = new Date(event.End);
    
    switch (selectedEventFilter.value) {
      case 'upcoming':
        // Future events (haven't started yet) or current events (started but not ended)
        return eventStart > now || (eventStart <= now && eventEnd >= now);
      case 'recent':
        // Events that ended in the last 30 days (but are not current/upcoming)
        return eventStart < now  && eventStart >= thirtyDaysAgo && eventEnd < now;
      case 'previous':
        // Events that ended more than 30 days ago
        return eventStart < thirtyDaysAgo;
      default:
        return true;
    }
  });
});

async function fetchEvents() {
  try {
    console.log('Fetching events...');
    const response = await fetch('https://fpvtrackside.com/api/public/events');
    const data = await response.json();
    events.value = data.map((event: EventType) => ({
      ...event,
      Club: {
        ...event.Club,
        LogoUrl: event.Club.LogoUrl ? `https://fpvtrackside.com${event.Club.LogoUrl}` : 'https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png'
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
    channels.value.forEach((channel) => {
      channel.DisplayName = channel.ShortBand + channel.Number;
    });
  } catch (error) {
    console.error('Error fetching channels:', error);
  }
}

function formatDate(dateString: string): string {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
}

async function refreshData() {
  refreshing.value = true;
  try {
    await fetchEvents();
  } finally {
    refreshing.value = false;
  }
}

// Expose refreshData method to parent component
defineExpose({
  refreshData
});

onMounted(() => {
  console.log('Explore mounted');
  fetchEvents();
  fetchChannels();
});
</script>

<template>
  <GridLayout rows="auto, *">
    <!-- Event Filter Buttons -->
    <GridLayout row="0" columns="*, *, *" class="p-2 bg-gray-100">
      <Button col="0" text="📅 Upcoming" 
              :class="selectedEventFilter === 'upcoming' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-black'"
              class="mx-1 py-1 text-xs rounded"
              @tap="selectedEventFilter = 'upcoming'" />
      <Button col="1" text="🕒 Recent" 
              :class="selectedEventFilter === 'recent' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-black'"
              class="mx-1 py-1 text-xs rounded"
              @tap="selectedEventFilter = 'recent'" />
      <Button col="2" text="📁 Previous" 
              :class="selectedEventFilter === 'previous' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-black'"
              class="mx-1 py-1 text-xs rounded"
              @tap="selectedEventFilter = 'previous'" />
    </GridLayout>

    <!-- Events List -->
    <GridLayout row="1">
      <ListView v-if="!loading && filteredEvents.length > 0" :items="filteredEvents">
        <template #default="{ item: event }">
          <GridLayout class="px-3">
            <ClubEventHeader row="0" :event="event" :formatDate="formatDate"
              @tap="$navigateTo(EventDetails, { props: { event, channels } })" class="mt-3 rounded-md" />
          </GridLayout>
        </template>
      </ListView>
      <Label v-else-if="!loading && filteredEvents.length === 0" 
             :text="'No ' + selectedEventFilter + ' events found'" 
             class="text-center text-gray-500" />
      <Label v-else text="Loading events..." class="text-center text-gray-500" />
    </GridLayout>
  </GridLayout>
</template>