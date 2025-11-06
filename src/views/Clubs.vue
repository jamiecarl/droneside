<script lang="ts" setup>
import {
  ref,
  onMounted,
  computed,
  $navigateTo,
} from 'nativescript-vue';
import { ClubType } from 'types/events.vue';
import ClubDetails from '~/views/ClubDetails.vue';

const clubs = ref<ClubType[]>([]);
const loadingClubs = ref(true);
const refreshing = ref(false);
const searchText = ref('');

// Computed property for filtered clubs
const filteredClubs = computed(() => {
  if (!searchText.value.trim()) {
    return clubs.value;
  }
  
  const searchTerm = searchText.value.toLowerCase().trim();
  return clubs.value.filter(club => 
    club.Name.toLowerCase().includes(searchTerm)
  );
});

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
  <GridLayout rows="auto, *">
    <!-- Search Field -->
    <GridLayout row="0" columns="*, auto" class="p-3 bg-gray-100">
      <TextField col="0" v-model="searchText" 
                 hint="Search clubs by name..."
                 class="bg-white border border-gray-300 rounded-lg px-3 py-2 mr-2" />
      <Label col="1" text="✕" 
             :class="searchText.trim() ? 'text-gray-600' : 'text-gray-300'"
             class="text-xl py-2 px-3" 
             verticalAlignment="center"
             @tap="searchText.trim() ? searchText = '' : null" />
    </GridLayout>

    <!-- Clubs List -->
    <GridLayout row="1">
      <ListView v-if="!loadingClubs && filteredClubs.length > 0" :items="filteredClubs">
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
      <Label v-else-if="!loadingClubs && filteredClubs.length === 0 && searchText.trim()" 
             :text="`No clubs found matching &quot;${searchText}&quot;`" 
             class="text-center text-gray-500" />
      <Label v-else-if="!loadingClubs && clubs.length === 0" 
             text="No clubs found" 
             class="text-center text-gray-500" />
      <Label v-else-if="loadingClubs" text="Loading clubs..." class="text-center text-gray-500" />
    </GridLayout>
  </GridLayout>
</template>