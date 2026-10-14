import { createApp } from 'nativescript-vue';
import Home from './views/Home.vue';
import { Application } from '@nativescript/core';
import CanvasPlugin from '@nativescript-community/ui-canvas/vue';

Application.on(Application.resumeEvent, () => {
    console.log('App resumed');
});

createApp(Home).use(CanvasPlugin).start();
