<script lang="ts" setup>
import {
  ref,
  onMounted,
  $navigateTo,
} from 'nativescript-vue';
import { ClubType } from 'types/events.vue';
import ClubDetails from '~/views/ClubDetails.vue';

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
        <GridLayout class="px-3">
          <GridLayout columns="auto, *" class="bg-white p-4 mt-3 rounded-md"
                      @tap="$navigateTo(ClubDetails, { props: { clubId: club.ID } })">
            <Image v-if="club.LogoUrl !== null" col="0" :src="club.LogoUrl" class="h-24 w-24 object-cover rounded-lg mr-3" />
            <Image v-else col="0" src="https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png" class="h-24 w-24 object-cover rounded-lg mr-3" />
            <StackLayout col="1">
              <Label :text="club.Name" class="text-md font-bold text-black mb-2" />
              <Label :text="club.Timezone" class="text-sm text-gray-400" />
            </StackLayout>
          </GridLayout>
        </GridLayout>
      </template>
    </ListView>
    <Label v-else-if="!loadingClubs && clubs.length === 0" 
           text="No clubs found" 
           class="text-center text-gray-500" />
    <Label v-else text="Loading clubs..." class="text-center text-gray-500" />
  </GridLayout>
</template>