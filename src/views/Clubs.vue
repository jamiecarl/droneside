<script lang="ts" setup>
import {
  ref,
  onMounted,
} from 'nativescript-vue';
import { ClubType } from 'types/events.vue';

const clubs = ref<ClubType[]>([]);
const loadingClubs = ref(true);
const refreshing = ref(false);

async function fetchClubs() {
  try {
    console.log('Fetching clubs...');
    const response = await fetch('https://fpvtrackside.com/api/public/clubs');
    const data = await response.json();
    clubs.value = data.map((club: ClubType) => ({
      ...club,
      LogoUrl: club.LogoUrl ? `https://fpvtrackside.com${club.LogoUrl}` : null
    })).sort((a: ClubType, b: ClubType) => a.Name.localeCompare(b.Name));
  } catch (error) {
    console.error('Error fetching clubs:', error);
  } finally {
    loadingClubs.value = false;
  }
}

async function refreshData() {
  refreshing.value = true;
  try {
    await fetchClubs();
  } finally {
    refreshing.value = false;
  }
}

// Expose refreshData method to parent component
defineExpose({
  refreshData
});

onMounted(() => {
  console.log('Clubs mounted');
  fetchClubs();
});
</script>

<template>
  <GridLayout>
    <ListView v-if="!loadingClubs && clubs.length > 0" :items="clubs">
      <template #default="{ item: club }">
        <GridLayout class="px-3 py-3 bg-white mb-2 mx-2 rounded-md" columns="auto, *" rows="auto">
          <Image v-if="club.LogoUrl !== null" col="0" :src="club.LogoUrl" class="h-16 w-16 object-cover rounded-lg mr-3" />
          <Image v-else col="0" src="https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png" class="h-16 w-16 object-cover rounded-lg mr-3" />
          <StackLayout col="1" class="bg-transparent">
            <Label :text="club.Name" class="text-lg font-bold text-black" />
            <Label :text="club.Timezone" class="text-sm text-gray-500" />
          </StackLayout>
        </GridLayout>
      </template>
    </ListView>
    <Label v-else-if="!loadingClubs && clubs.length === 0" 
           text="No clubs found" 
           class="text-center text-gray-500" />
    <Label v-else text="Loading clubs..." class="text-center text-gray-500" />
  </GridLayout>
</template>