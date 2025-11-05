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
import { TabView, TabViewItem, ContentView } from '@nativescript/core';

const events = ref<EventType[]>([]);
const channels = ref<ChannelType[]>([]);
const loading = ref(true);
const refreshing = ref(false);
const selectedTabIndex = ref(0);

// Date helpers
const now = new Date();
const thirtyDaysAgo = new Date(now.getTime() - (30 * 24 * 60 * 60 * 1000));

// Computed filtered events based on selected tab
const upcomingEvents = computed(() => {
  return events.value.filter(event => {
    const eventStart = new Date(event.Start);
    const eventEnd = new Date(event.End);
    // Future events (haven't started yet) or current events (started but not ended)
    return eventStart > now || (eventStart <= now && eventEnd >= now);
  });
});

const recentEvents = computed(() => {
  return events.value.filter(event => {
    const eventStart = new Date(event.Start);
    const eventEnd = new Date(event.End);
    // Events that ended in the last 30 days (but are not current/upcoming)
    return eventStart < now && eventStart >= thirtyDaysAgo && eventEnd < now;
  });
});

const previousEvents = computed(() => {
  return events.value.filter(event => {
    const eventStart = new Date(event.Start);
    // Events that ended more than 30 days ago
    return eventStart < thirtyDaysAgo;
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

function onTabChange(index: any) {
  selectedTabIndex.value = index.value;
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
  <GridLayout rows="*">
    <TabView @selectedIndexChange="onTabChange">
      <TabViewItem title="Upcoming">
        <GridLayout>
          <ListView v-if="!loading && upcomingEvents.length > 0" :items="upcomingEvents">
            <template #default="{ item: event }">
              <GridLayout class="px-3">
                <ClubEventHeader row="0" :event="event" :formatDate="formatDate"
                  @tap="$navigateTo(EventDetails, { props: { event, channels } })" class="mt-3 rounded-md" />
              </GridLayout>
            </template>
          </ListView>
          <Label v-else-if="!loading && upcomingEvents.length === 0" 
                 text="No upcoming events found" 
                 class="text-center text-gray-500" />
          <Label v-else text="Loading events..." class="text-center text-gray-500" />
        </GridLayout>
      </TabViewItem>
      
      <TabViewItem title="Recent">
        <GridLayout>
          <ListView v-if="!loading && recentEvents.length > 0" :items="recentEvents">
            <template #default="{ item: event }">
              <GridLayout class="px-3">
                <ClubEventHeader row="0" :event="event" :formatDate="formatDate"
                  @tap="$navigateTo(EventDetails, { props: { event, channels } })" class="mt-3 rounded-md" />
              </GridLayout>
            </template>
          </ListView>
          <Label v-else-if="!loading && recentEvents.length === 0" 
                 text="No recent events found" 
                 class="text-center text-gray-500" />
          <Label v-else text="Loading events..." class="text-center text-gray-500" />
        </GridLayout>
      </TabViewItem>
      
      <TabViewItem title="Previous">
        <GridLayout>
          <ListView v-if="!loading && previousEvents.length > 0" :items="previousEvents">
            <template #default="{ item: event }">
              <GridLayout class="px-3">
                <ClubEventHeader row="0" :event="event" :formatDate="formatDate"
                  @tap="$navigateTo(EventDetails, { props: { event, channels } })" class="mt-3 rounded-md" />
              </GridLayout>
            </template>
          </ListView>
          <Label v-else-if="!loading && previousEvents.length === 0" 
                 text="No previous events found" 
                 class="text-center text-gray-500" />
          <Label v-else text="Loading events..." class="text-center text-gray-500" />
        </GridLayout>
      </TabViewItem>
    </TabView>
  </GridLayout>
</template>