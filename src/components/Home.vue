<script lang="ts" setup>
import {
  ref,
  onMounted,
  $navigateTo,
} from 'nativescript-vue';
import Details from './Details.vue';

interface ClubType {
  ID: string;
  Name: string;
  LogoUrl: string;
}

interface EventType {
  ID: string;
  Start: string;
  End: string;
  Name: string;
  PilotCount: number;
  Club: ClubType;
  BannerUrl: string;
}

const events = ref<EventType[]>([]);
const loading = ref(true);

async function fetchEvents() {
  try {
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
  fetchEvents();
});
</script>

<template>
  <Frame>
    <Page>
      <ActionBar>
        <Label text="DroneSide" class="font-bold text-lg" />
      </ActionBar>

      <GridLayout rows="*, auto, auto, *" class="px-4" height="100%">
        <ListView v-if="!loading" :items="events" row="1" height="100%">
          <template #default="{ item: event }">
            <GridLayout columns="auto, *" class="p-4 m-2 bg-white border-2 border-gray-300 rounded-lg"
              @tap="$navigateTo(Details, { props: { event } })">
              <Image col="0" :src="event.Club.LogoUrl" class="h-20 w-20 object-cover rounded-lg mr-2" />
              <StackLayout col="1">
                <Label :text="event.Name" class="text-lg font-bold text-gray-500" />
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

<style>
GridLayout {
  color: #fff;
  padding: 1rem;
}

ListView {
  color: #fff;
  border: 5px solid red;
}
</style>
