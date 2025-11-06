import { NativeScriptConfig } from '@nativescript/core';

export default {
  id: 'org.jamiecarl.droneside',
  appPath: 'src',
  appResourcesPath: 'App_Resources',
  android: {
    v8Flags: '--expose_gc',
    markingMode: 'none',
    versionCode: 2
  }
} as NativeScriptConfig;