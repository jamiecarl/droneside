import { ApplicationSettings } from '@nativescript/core';

const FAVORITE_CLUB_KEY = 'favoriteClubId';

export function setFavoriteClub(clubId: string): void {
  ApplicationSettings.setString(FAVORITE_CLUB_KEY, clubId);
}

export function getFavoriteClub(): string | null {
  return ApplicationSettings.getString(FAVORITE_CLUB_KEY) || null;
}

export function removeFavoriteClub(): void {
  ApplicationSettings.remove(FAVORITE_CLUB_KEY);
}

export function isFavoriteClub(clubId: string): boolean {
  const favoriteClubId = getFavoriteClub();
  return favoriteClubId === clubId;
}