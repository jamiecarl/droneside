import { createApp } from 'nativescript-vue';
import Home from './views/Home.vue';
import { Application } from '@nativescript/core';

Application.on(Application.resumeEvent, () => {
    console.log('App resumed');
});

createApp(Home).start();
