<script lang="ts" setup>
import {
  ref,
  onMounted,
  $navigateBack,
} from 'nativescript-vue';
import { ClubType } from 'types/events.vue';

const props = defineProps<{
  clubId: string;
}>();

const club = ref<ClubType | null>(null);
const loading = ref(true);

async function fetchClubDetails() {
  try {
    console.log(`Fetching club details for ID: ${props.clubId}`);
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

function formatDate(dateString: string): string {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
}

function goBack() {
  $navigateBack();
}

onMounted(() => {
  fetchClubDetails();
});
</script>

<template>
  <Page>
    <ActionBar :title="club?.Name || 'Club Details'" class="bg-red-600 text-white">
      <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="goBack" />
    </ActionBar>

    <ScrollView>
      <StackLayout v-if="!loading && club">
        <!-- Banner with Logo Overlay -->
        <GridLayout class="relative">
          <Image v-if="club.BannerUrl" :src="club.BannerUrl" class="w-full h-48 object-cover" />
          <GridLayout v-else class="w-full h-48 bg-gray-300" />
          <Image v-if="club.LogoUrl" :src="club.LogoUrl" class="h-20 w-20 object-cover rounded-lg ml-4 mt-4" 
                 verticalAlignment="top" horizontalAlignment="left" />
          <Image v-else src="https://fpvtrackside.com/assets/defaultclub-iJLq4-Em.png" 
                 class="h-20 w-20 object-cover rounded-lg ml-4 mt-4" 
                 verticalAlignment="top" horizontalAlignment="left" />
        </GridLayout>
        
        <!-- Club Header -->
        <StackLayout class="p-4 pb-6">
          <Label :text="club.Name" class="text-2xl font-bold text-black mb-2" textWrap="true" />
          <Label :text="'Founded: ' + formatDate(club.Creation)" class="text-sm text-gray-500" />
        </StackLayout>

        <!-- Club Details Cards -->
        
        <!-- Location Information -->
        <StackLayout class="bg-white p-4 rounded-lg mb-4 shadow mx-4">
          <Label text="📍 Location" class="text-lg font-semibold text-gray-700 mb-3" />
          <Label v-if="club.Address" :text="club.Address" class="text-base text-gray-600 mb-2" textWrap="true" />
          <Label :text="'Timezone: ' + club.Timezone" class="text-sm text-gray-500" />
          <GridLayout v-if="club.Latitude && club.Longitude" columns="*, *" class="mt-2">
            <Label col="0" :text="'Lat: ' + club.Latitude.toFixed(6)" class="text-xs text-gray-400" />
            <Label col="1" :text="'Lng: ' + club.Longitude.toFixed(6)" class="text-xs text-gray-400" />
          </GridLayout>
        </StackLayout>

        <!-- Branding Information -->
        <StackLayout class="bg-white p-4 rounded-lg mb-4 shadow mx-4">
          <Label text="🎨 Branding" class="text-lg font-semibold text-gray-700 mb-3" />
          <GridLayout columns="*, *" class="mb-2">
            <StackLayout col="0" class="mr-2">
              <Label text="Primary Color" class="text-sm text-gray-500 mb-1" />
              <GridLayout class="h-8 rounded" :style="'background-color: ' + club.PrimaryColor">
                <Label :text="club.PrimaryColor" class="text-xs text-center" :style="'color: ' + club.TextColor" />
              </GridLayout>
            </StackLayout>
            <StackLayout col="1" class="ml-2">
              <Label text="Text Color" class="text-sm text-gray-500 mb-1" />
              <GridLayout class="h-8 rounded border border-gray-300" :style="'background-color: ' + club.TextColor">
                <Label :text="club.TextColor" class="text-xs text-center" :style="'color: ' + (club.TextColor === '#FFFFFF' || club.TextColor === '#ffffff' ? '#000000' : '#FFFFFF')" />
              </GridLayout>
            </StackLayout>
          </GridLayout>
        </StackLayout>

        <!-- Status Information -->
        <StackLayout class="bg-white p-4 rounded-lg mb-4 shadow mx-4">
          <Label text="ℹ️ Information" class="text-lg font-semibold text-gray-700 mb-3" />
          <Label text="Club ID" class="text-sm text-gray-500" />
          <Label :text="club.ID" class="text-base text-gray-700 mb-2 font-mono" />
          <Label text="Visibility Status" class="text-sm text-gray-500" />
          <Label :text="club.Visible ? '✅ Public' : '❌ Private'" class="text-base text-gray-700" />
        </StackLayout>

      </StackLayout>
      
      <StackLayout v-else-if="loading" class="p-8">
        <Label text="Loading club details..." class="text-center text-gray-500" />
      </StackLayout>
      
      <StackLayout v-else class="p-8">
        <Label text="Unable to load club details" class="text-center text-gray-500" />
      </StackLayout>
    </ScrollView>
  </Page>
</template>