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
  <GridLayout rows="auto, *" class="bg-ds-base">
    <!-- Search Field -->
    <GridLayout row="0" columns="*, auto" class="p-4 pb-3 bg-ds-base"
                :class="searchText.trim() ? 'border border-ds-cyan rounded-xl' : 'border border-ds-line rounded-xl'">
      <TextField col="0" v-model="searchText"
                 hint="Search clubs by name..."
                 class="text-ds-ink px-3 py-2" />
      <Label col="1" class="fa text-lg py-2 px-3" text="&#xf00d;"
             :class="searchText.trim() ? 'text-ds-cyan' : 'text-ds-faint'"
             verticalAlignment="center"
             @tap="searchText.trim() ? searchText = '' : null" />
    </GridLayout>

    <!-- Clubs List -->
    <GridLayout row="1" class="bg-ds-base">
      <ListView v-if="!loadingClubs && filteredClubs.length > 0" :items="filteredClubs" class="bg-ds-base">
        <template #default="{ item: club }">
          <GridLayout class="px-4">
            <GridLayout columns="auto, *" class="bg-ds-surface border border-ds-line rounded-2xl p-3.5 mt-2.5"
                        @tap="$navigateTo(ClubDetails, { props: { clubId: club.ID } })">
              <Image v-if="club.LogoUrl !== null" col="0" :src="club.LogoUrl" class="h-11 w-11 object-cover rounded-xl mr-3" />
              <Image v-else col="0" src="https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png" class="h-11 w-11 object-cover rounded-xl mr-3" />
              <StackLayout col="1" verticalAlignment="center">
                <Label :text="club.Name" class="text-base font-semibold text-ds-ink mb-0.5" textWrap="true" />
                <Label :text="club.Timezone" class="font-mono-ds text-xs text-ds-muted" />
              </StackLayout>
            </GridLayout>
          </GridLayout>
        </template>
      </ListView>
      <Label v-else-if="!loadingClubs && filteredClubs.length === 0 && searchText.trim()"
             :text="`No clubs found matching &quot;${searchText}&quot;`"
             class="text-center text-ds-muted mt-8" />
      <Label v-else-if="!loadingClubs && clubs.length === 0"
             text="No clubs found"
             class="text-center text-ds-muted mt-8" />
      <Label v-else-if="loadingClubs" text="Loading clubs..." class="text-center text-ds-muted mt-8" />
    </GridLayout>
  </GridLayout>
</template>