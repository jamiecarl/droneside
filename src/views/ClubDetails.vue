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
import { setHomeClub, getHomeClub, removeHomeClub, isHomeClub } from '~/utils/storage';

const props = defineProps<{
  clubId: string;
}>();

const club = ref<ClubType | null>(null);
const clubEvents = ref<EventType[]>([]);
const channels = ref<ChannelType[]>([]);
const loading = ref(true);
const loadingEvents = ref(false);
const isStarred = ref(false);

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

function toggleHomeClub() {
  if (isStarred.value) {
    removeHomeClub();
    isStarred.value = false;
    console.log('Removed home club');
  } else {
    setHomeClub(props.clubId);
    isStarred.value = true;
    console.log('Set home club:', props.clubId);
  }
}

function checkIfHomeClub() {
  isStarred.value = isHomeClub(props.clubId);
}

onMounted(() => {
  fetchClubDetails();
  fetchChannels();
  checkIfHomeClub();
});
</script>

<template>
  <Page class="bg-ds-base">
    <ActionBar class="bg-ds-base">
      <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="goBack" />
      <Label text="Club" class="font-bold text-lg text-ds-ink" />
      <ActionItem @tap="toggleHomeClub">
        <Label class="fa text-center"
               :class="isStarred ? 'text-ds-cyan text-2xl' : 'text-ds-faint text-2xl'"
               text="&#xf005;"
               width="50" height="40"
               paddingRight="16"
               verticalAlignment="center"
               horizontalAlignment="center" />
      </ActionItem>
    </ActionBar>

    <GridLayout rows="auto, *" class="p-0 bg-ds-base" v-if="!loading && club">
      <!-- Logo and Club Header -->
      <GridLayout row="0" columns="auto, *" class="px-5 pt-3 pb-4">
        <Image v-if="club.LogoUrl" :src="club.LogoUrl.startsWith('/') ? 'https://fpvtrackside.com' + club.LogoUrl : club.LogoUrl" col="0"
               class="h-14 w-14 object-cover rounded-2xl mr-3.5" />
        <Image v-else src="https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png" col="0"
               class="h-14 w-14 object-cover rounded-2xl mr-3.5" />
        <Label col="1" :text="club.Name" class="text-2xl font-bold text-ds-ink" verticalAlignment="center" textWrap="true" />
      </GridLayout>

      <ContentView row="1" class="bg-ds-base">
        <TabView class="ds-tabview" @selectedIndexChange="onTabChange">
          <TabViewItem title="Overview">
            <GridLayout class="bg-ds-base">
              <ScrollView row="1">
                <StackLayout class="p-4 bg-ds-base">
                  <!-- Founded Date -->
                  <StackLayout class="bg-ds-surface border border-ds-line p-4 rounded-2xl mb-3">
                    <Label class="font-mono-ds text-[10px] tracking-wide text-ds-muted2 mb-2">
                      <Span class="fa" text="&#xf073;" />
                      <Span text="  FOUNDED" />
                    </Label>
                    <Label :text="formatDate(club.Creation)" class="text-base text-ds-ink2" />
                  </StackLayout>

                  <!-- Location Information -->
                  <StackLayout class="bg-ds-surface border border-ds-line p-4 rounded-2xl mb-3">
                    <Label class="font-mono-ds text-[10px] tracking-wide text-ds-muted2 mb-2">
                      <Span class="fa" text="&#xf041;" />
                      <Span text="  LOCATION" />
                    </Label>
                    <Label v-if="club.Address" :text="club.Address" class="text-base text-ds-ink2 mb-2" textWrap="true" />
                    <Label :text="'Timezone: ' + club.Timezone" class="text-sm text-ds-muted mb-2" />
                    <GridLayout v-if="club.Latitude && club.Longitude" columns="*, auto" class="mt-2 p-3 bg-ds-surface2 rounded-xl">
                      <StackLayout col="0">
                        <Label text="COORDINATES" class="font-mono-ds text-[10px] tracking-wide text-ds-muted2" />
                        <Label :text="club.Latitude.toFixed(6) + ', ' + club.Longitude.toFixed(6)" class="font-mono-ds text-xs text-ds-muted mt-1" />
                      </StackLayout>
                      <Button col="1" text="Open Map" class="bg-ds-cyan text-[#04222B] text-xs font-semibold px-3 py-1 rounded-lg"
                              @tap="openMap(club.Latitude, club.Longitude)" />
                    </GridLayout>
                  </StackLayout>
                </StackLayout>
              </ScrollView>
            </GridLayout>
          </TabViewItem>

          <TabViewItem title="Events">
            <GridLayout class="bg-ds-base">
              <ScrollView row="1" v-if="!loadingEvents">
                <StackLayout v-if="clubEvents.length > 0" class="px-4 pt-4">
                  <GridLayout v-for="event in clubEvents" :key="event.ID" class="mb-2.5">
                    <ClubEventHeader :event="event" :formatDate="formatDate"
                      @tap="$navigateTo(EventDetails, { props: { event, channels } })" />
                  </GridLayout>
                </StackLayout>
                <Label v-else text="No events found for this club" class="text-ds-muted text-center p-8" />
              </ScrollView>
              <ActivityIndicator row="1" v-else busy="true" class="h-16 w-16" />
            </GridLayout>
          </TabViewItem>
        </TabView>
      </ContentView>
    </GridLayout>

    <StackLayout v-else-if="loading" class="p-8">
      <Label text="Loading club details..." class="text-center text-ds-muted" />
    </StackLayout>

    <StackLayout v-else class="p-8">
      <Label text="Unable to load club details" class="text-center text-ds-muted" />
    </StackLayout>
  </Page>
</template>