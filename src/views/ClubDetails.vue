<script lang="ts" setup>
import {
  ref,
  onMounted,
  computed,
  $navigateBack,
  $navigateTo,
} from 'nativescript-vue';
import { ClubType, EventType, ChannelType } from 'types/events.vue';
import ClubEventHeader from '~/components/ClubEventHeader.vue';
import EventDetails from '~/views/EventDetails.vue';
import { TabView, TabViewItem, ContentView } from '@nativescript/core';

const props = defineProps<{
  clubId: string;
}>();

const club = ref<ClubType | null>(null);
const clubEvents = ref<EventType[]>([]);
const channels = ref<ChannelType[]>([]);
const loading = ref(true);
const loadingEvents = ref(false);

async function fetchClubDetails() {
  try {
    const response = await fetch(`https://fpvtrackside.com/api/public/clubs/id/${props.clubId}`);
    const data = await response.json();
    club.value = {
      ...data,
      LogoUrl: data.LogoUrl ? `https://fpvtrackside.com${data.LogoUrl}` : null,
      BannerUrl: data.BannerUrl ? `https://fpvtrackside.com${data.BannerUrl}` : null
    };
  } catch (error) {
    console.error('Error fetching club details:', error);
  } finally {
    loading.value = false;
  }
}

async function fetchClubEvents() {
  if (loadingEvents.value) return;
  
  loadingEvents.value = true;
  try {
    const response = await fetch(`https://fpvtrackside.com/api/public/clubs/id/${props.clubId}/full`);
    const data = await response.json();
    
    // Check if data is an array or has events property
    let eventsArray = [];
    if (Array.isArray(data)) {
      eventsArray = data;
    } else if (data.events && Array.isArray(data.events)) {
      eventsArray = data.events;
    } else if (data.Events && Array.isArray(data.Events)) {
      eventsArray = data.Events;
    } else {
      clubEvents.value = [];
      return;
    }
    
    clubEvents.value = eventsArray.map((event: EventType) => {
      // Since all events are from this club, use the main club's LogoUrl
      let logoUrl = club.value?.LogoUrl;
      
      // Fallback to event's club LogoUrl if main club LogoUrl not available
      if (!logoUrl) {
        const eventClub = event.Club as any;
        logoUrl = eventClub.LogoUrl;
      }
      
      let processedLogoUrl;
      if (!logoUrl) {
        processedLogoUrl = 'https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png';
      } else if (logoUrl.startsWith('http')) {
        processedLogoUrl = logoUrl;
      } else {
        processedLogoUrl = `https://fpvtrackside.com${logoUrl}`;
      }
      
      return {
        ...event,
        Club: {
          ...event.Club,
          LogoUrl: processedLogoUrl
        }
      };
    }).sort((a: EventType, b: EventType) => new Date(b.Start).getTime() - new Date(a.Start).getTime());
  } catch (error) {
    console.error('Error fetching club events:', error);
    clubEvents.value = [];
  } finally {
    loadingEvents.value = false;
  }
}

async function fetchChannels() {
  try {
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

function getCurrentTimeInTimezone(timezone: string): string {
  // For now, just show the timezone name since proper timezone conversion
  // requires additional libraries that may not work well in NativeScript
  return `Timezone: ${timezone}`;
}

function formatDate(dateString: string): string {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
}

function goBack() {
  $navigateBack();
}

function openMap(latitude: number, longitude: number) {
  try {
    // Try to open the device's default map app with the coordinates
    const url = `geo:${latitude},${longitude}?q=${latitude},${longitude}`;
    // On Android, this will open Google Maps or the default map app
    // On iOS, this would need a different approach, but for now this works on Android
    console.log('Opening map with URL:', url);
    // For NativeScript, you might need to use utils.openUrl() or a similar method
    // utils.openUrl(url);
  } catch (error) {
    console.error('Error opening map:', error);
  }
}

function onTabChange(index: any) {
  if (index.value === 1 && clubEvents.value.length === 0) {
    fetchClubEvents();
  }
}

onMounted(() => {
  fetchClubDetails();
  fetchChannels();
});
</script>

<template>
  <Page>
    <ActionBar :title="club?.Name || 'Club Details'" class="bg-red-600 text-white">
      <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="goBack" />
    </ActionBar>

    <GridLayout rows="auto, *" class="p-0" v-if="!loading && club">
      <!-- Banner with Logo Overlay and Club Header -->
      <StackLayout row="0">
        <GridLayout class="relative">
          <Image v-if="club.BannerUrl" :src="club.BannerUrl" class="w-full h-48 object-cover" />
          <GridLayout v-else class="w-full h-48 bg-gray-300" />
          <Image v-if="club.LogoUrl" :src="club.LogoUrl" class="h-20 w-20 object-cover rounded-lg ml-4 mt-4" 
                 verticalAlignment="top" horizontalAlignment="left" />
          <Image v-else src="https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png" 
                 class="h-20 w-20 object-cover rounded-lg ml-4 mt-4" 
                 verticalAlignment="top" horizontalAlignment="left" />
        </GridLayout>
        
        <GridLayout class="p-3" :style="'background-color: ' + club.PrimaryColor">
          <Label :text="club.Name" class="text-2xl font-bold text-center" :style="'color: ' + club.TextColor" textWrap="true" />
        </GridLayout>
      </StackLayout>

      <ContentView row="1" class="bg-black rounded-t-3xl">
        <TabView @selectedIndexChange="onTabChange">
          <TabViewItem title="Details">
            <GridLayout>
              <ScrollView row="1">
                <StackLayout class="p-3 bg-black">
                  <!-- Founded Date -->
                  <StackLayout class="bg-gray-800 p-4 rounded-lg mb-4">
                    <Label text="📅 Founded" class="text-lg font-semibold text-white mb-3" />
                    <Label :text="formatDate(club.Creation)" class="text-base text-gray-300" />
                  </StackLayout>
                  
                  <!-- Location Information -->
                  <StackLayout class="bg-gray-800 p-4 rounded-lg mb-4">
                    <Label text="📍 Location" class="text-lg font-semibold text-white mb-3" />
                    <Label v-if="club.Address" :text="club.Address" class="text-base text-gray-300 mb-2" textWrap="true" />
                    <Label :text="'Timezone: ' + club.Timezone" class="text-sm text-gray-400 mb-2" />
                    <GridLayout v-if="club.Latitude && club.Longitude" columns="*, auto" class="mt-2 p-2 bg-gray-700 rounded">
                      <StackLayout col="0">
                        <Label text="Coordinates" class="text-sm font-semibold text-gray-300" />
                        <Label :text="club.Latitude.toFixed(6) + ', ' + club.Longitude.toFixed(6)" class="text-xs text-gray-400" />
                      </StackLayout>
                      <Button col="1" text="📍 Open Map" class="bg-blue-500 text-white text-xs px-3 py-1 rounded" 
                              @tap="openMap(club.Latitude, club.Longitude)" />
                    </GridLayout>
                  </StackLayout>
                </StackLayout>
              </ScrollView>
            </GridLayout>
          </TabViewItem>
          
          <TabViewItem title="Events">
            <GridLayout>
              <ScrollView row="1" v-if="!loadingEvents">
                <StackLayout v-if="clubEvents.length > 0">
                  <GridLayout v-for="event in clubEvents" :key="event.ID" class="px-3">
                    <ClubEventHeader :event="event" :formatDate="formatDate"
                      @tap="$navigateTo(EventDetails, { props: { event, channels } })" class="mt-3 rounded-md" />
                  </GridLayout>
                </StackLayout>
                <Label v-else text="No events found for this club" class="text-white text-center p-4" />
              </ScrollView>
              <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
            </GridLayout>
          </TabViewItem>
        </TabView>
      </ContentView>
    </GridLayout>

    <StackLayout v-else-if="loading" class="p-8">
      <Label text="Loading club details..." class="text-center text-gray-500" />
    </StackLayout>
    
    <StackLayout v-else class="p-8">
      <Label text="Unable to load club details" class="text-center text-gray-500" />
    </StackLayout>
  </Page>
</template>