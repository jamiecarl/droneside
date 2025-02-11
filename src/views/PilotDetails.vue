<script lang="ts" setup>
import { defineProps, ref, onMounted } from 'nativescript-vue';
import { EventRoundType } from 'types/events.vue';

const props = defineProps<{ round: EventRoundType }>();

onMounted(() => {
});
</script>

<template>
  <Page>
    <ActionBar>
      <NavigationButton text="Back" android.systemIcon="ic_menu_back" @tap="$navigateBack" />
      <Label text="Pilot Details" class="font-bold text-lg" />
    </ActionBar>
  </Page>
</template>
