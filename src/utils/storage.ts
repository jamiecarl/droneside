import { ApplicationSettings } from '@nativescript/core';

const HOME_CLUB_KEY = 'homeClubId';

export function setHomeClub(clubId: string): void {
  ApplicationSettings.setString(HOME_CLUB_KEY, clubId);
}

export function getHomeClub(): string | null {
  return ApplicationSettings.getString(HOME_CLUB_KEY) || null;
}

export function removeHomeClub(): void {
  ApplicationSettings.remove(HOME_CLUB_KEY);
}

export function isHomeClub(clubId: string): boolean {
  const homeClubId = getHomeClub();
  return homeClubId === clubId;
}