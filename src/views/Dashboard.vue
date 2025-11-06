<script lang="ts" setup>
import { ref, onMounted, computed, $navigateTo } from 'nativescript-vue';
import { EventType, ChannelType, ClubType } from 'types/events.vue';
import { getHomeClub } from '~/utils/storage';
import ClubEventHeader from '~/components/ClubEventHeader.vue';
import EventDetails from '~/views/EventDetails.vue';
import ClubDetails from '~/views/ClubDetails.vue';

const selectedClub = ref<ClubType | null>(null);
const clubEvents = ref<EventType[]>([]);
const channels = ref<ChannelType[]>([]);
const loading = ref(false);

async function loadHomeClubEvents() {
  const homeClubId = getHomeClub();
  if (!homeClubId) {
    return;
  }

  loading.value = true;
  try {
    // Fetch club details
    const clubResponse = await fetch(`https://fpvtrackside.com/api/public/clubs/id/${homeClubId}`);
    const clubData = await clubResponse.json();
    selectedClub.value = {
      ...clubData,
      LogoUrl: clubData.LogoUrl ? `https://fpvtrackside.com${clubData.LogoUrl}` : null,
      BannerUrl: clubData.BannerUrl ? `https://fpvtrackside.com${clubData.BannerUrl}` : null
    };

    // Fetch club events
    const eventsResponse = await fetch(`https://fpvtrackside.com/api/public/clubs/id/${homeClubId}/full`);
    const eventsData = await eventsResponse.json();
    
    let eventsArray = [];
    if (Array.isArray(eventsData)) {
      eventsArray = eventsData;
    } else if (eventsData.events && Array.isArray(eventsData.events)) {
      eventsArray = eventsData.events;
    } else if (eventsData.Events && Array.isArray(eventsData.Events)) {
      eventsArray = eventsData.Events;
    }
    
    clubEvents.value = eventsArray.map((event: EventType) => ({
      ...event,
      Club: {
        ...event.Club,
        LogoUrl: selectedClub.value?.LogoUrl || 'https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png'
      }
    })).sort((a: EventType, b: EventType) => new Date(b.Start).getTime() - new Date(a.Start).getTime());

    // Fetch channels
    const channelsResponse = await fetch('https://fpvtrackside.com/api/public/channels');
    const channelsData = await channelsResponse.json();
    channels.value = channelsData;
    channels.value.forEach((channel) => {
      channel.DisplayName = channel.ShortBand + channel.Number;
    });
  } catch (error) {
    console.error('Error loading home club events:', error);
  } finally {
    loading.value = false;
  }
}

function formatDate(dateString: string): string {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
}

// Computed to get recent events (last 30 days) and upcoming events
const recentAndUpcomingEvents = computed(() => {
  const now = new Date();
  const thirtyDaysAgo = new Date(now.getTime() - (30 * 24 * 60 * 60 * 1000));
  
  return clubEvents.value.filter(event => {
    const eventStart = new Date(event.Start);
    const eventEnd = new Date(event.End);
    
    // Show current events, upcoming events, or events from the last 30 days
    return eventStart > thirtyDaysAgo || eventEnd >= now;
  }).slice(0, 5); // Limit to 5 events
});

function refreshData() {
  loadHomeClubEvents();
}

function goToClubDetails() {
  if (selectedClub.value) {
    $navigateTo(ClubDetails, { props: { clubId: selectedClub.value.ID } });
  }
}

// Expose refreshData method to parent component
defineExpose({
  refreshData
});

onMounted(() => {
  loadHomeClubEvents();
});
</script>

<template>
  <GridLayout rows="*">
    <StackLayout v-if="!selectedClub && !loading" class="p-8 text-center">
      <!-- Welcome Icon -->
      <Label text="⌂" class="text-8xl mb-6 text-blue-500" />
      
      <!-- Welcome Message -->
      <Label text="Welcome to DroneSide" class="text-3xl font-bold text-white mb-4" textWrap="true" />
      
      <!-- Getting Started -->
      <StackLayout class="bg-gray-100 p-6 rounded-lg mb-6 mt-8" style="padding-top: 24; padding-bottom: 24;">
        <Label text="Getting Started" class="text-lg font-semibold text-gray-700 mb-6" />
        <Label text="Browse events in the Explore tab or select a club from the Clubs tab to see personalized content here." class="text-base text-gray-600 text-center" textWrap="true" style="line-height: 1.3;" />
      </StackLayout>
    </StackLayout>

    <GridLayout v-else-if="selectedClub" rows="auto, *">
      <!-- Home Club Header -->
      <GridLayout row="0" columns="auto, *" class="p-4 bg-gray-800 mb-4">
        <Image col="0" v-if="selectedClub.LogoUrl" :src="selectedClub.LogoUrl" 
               class="w-32 object-cover rounded-lg mr-4" 
               verticalAlignment="center" horizontalAlignment="left"
               @tap="goToClubDetails" />
        <Image col="0" v-else src="https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png" 
               class="w-16 object-cover rounded-lg mr-4" 
               verticalAlignment="center" horizontalAlignment="left"
               @tap="goToClubDetails" />
        
        <StackLayout col="1" verticalAlignment="center">
          <Label text="★ Your Home Club" class="text-yellow-400 text-lg font-bold mb-1" />
          <Label :text="selectedClub.Name" class="text-white text-xl font-bold mb-1" textWrap="true" style="line-height: 1.2;" />
          <Label v-if="recentAndUpcomingEvents.length > 0" :text="`${recentAndUpcomingEvents.length} Recent & Upcoming Events`" class="text-gray-300 text-sm" />
          <Label v-else text="No recent events" class="text-gray-300 text-sm" />
        </StackLayout>
      </GridLayout>

      <!-- Club Events List -->
      <ScrollView row="1" v-if="recentAndUpcomingEvents.length > 0">
        <StackLayout class="px-3">
          <GridLayout v-for="event in recentAndUpcomingEvents" :key="event.ID" class="mb-3">
            <ClubEventHeader :event="event" :formatDate="formatDate"
              @tap="$navigateTo(EventDetails, { props: { event, channels } })" class="rounded-md" />
          </GridLayout>
        </StackLayout>
      </ScrollView>

      <StackLayout v-else row="1" class="p-8 text-center">
        <Label text="◯" class="text-6xl mb-4 text-gray-500" />
        <Label text="No events to show" class="text-xl text-gray-500 mb-2" />
        <Label text="Check the Explore tab for more events" class="text-sm text-gray-400" textWrap="true" />
      </StackLayout>
    </GridLayout>

    <StackLayout v-else-if="loading" class="p-8 text-center">
      <ActivityIndicator busy="true" class="h-16 w-16 mb-4" />
      <Label text="Loading your home club..." class="text-center text-gray-500" />
    </StackLayout>
  </GridLayout>
</template>