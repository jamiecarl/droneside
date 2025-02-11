export interface ClubType {
    ID: string;
    Name: string;
    LogoUrl: string;
}

export interface EventType {
    ID: string;
    Start: string;
    End: string;
    Name: string;
    PilotCount: number;
    Club: ClubType;
    BannerUrl: string;
}

export interface RoundType {
    ID: string;
    RoundNumber: string;
    Name: string;
    Event: string;
    RoundOrder: string;
    RoundType: string;
    EventType: string;
}

export interface PilotType {
    ID: string;
    Name: string;
    PhotoURL: string | null;
}

export interface ResultSummaryType {
    ID: string;
    Pilot: string;
    Race: string;
    HoleshotTime: string;
    PbLapTime: string;
    PbLapCount: string;
    TargetLapTime: string;
    TargetLapCount: string;
    RaceTime: string;
    LapCount: string;
    Points: string;
    Position: string;
}

export interface RaceDetailType {
    ID: string;
    Start: string;
    End: string;
    Name: string;
    Event: string;
    Round: string;
    RaceNumber: number;
    Valid: boolean;
    TargetLaps: string;
    PrimaryTimingSystemLocation: string;
    Bracket: string;
    ResultSummaries: ResultSummaryType[];
}