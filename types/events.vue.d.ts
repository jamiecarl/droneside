export interface ChannelType {
    Number: number;
    Band: string;
    ChannelPrefix: string;
    Frequency: number;
    DisplayName: string | null;
    ID: string;
    ExternalID: number;
    ShortBand: string;
}

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
    Phonetic: string;
    FirstName: string;
    LastName: string;
    PhotoURL: string;
    CatchPhrase: string;
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

export interface PilotChannelType {
    ID: string;
    Race: string;
    Channel: string;
    Pilot: string;
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
    PilotChannels: PilotChannelType[];
    ResultSummaries: ResultSummaryType[];
}

export interface RawRaceDataType {
    ID: string;
    RaceNumber: number;
    Round: string;
    TargetLaps: number;
    PrimaryTimingSystemLocation: string;
    Valid: boolean;
    AutoAssignNumbers: boolean;
    Event: string;
    Bracket: string;
    ExternalID: number;
    Start: string;
    End: string;
    TotalPausedTime: string;
    PilotChannels: PilotChannelType[];
    Detections: DetectionType[];
    Laps: LapType[];
}

export interface LapType {
    Detection: string;
    LengthSeconds: number;
    LapNumber: number;
    StartTime: string;
    EndTime: string;
    ID: string;
    ExternalID: number;
}

export interface DetectionType {
    TimingSystemIndex: number;
    Channel: string;
    Time: string;
    Peak: number;
    TimingSystemType: string;
    Pilot: string;
    LapNumber: number;
    Valid: boolean;
    ValidityType: string;
    IsLapEnd: boolean;
    RaceSector: number;
    IsHoleshot: boolean;
    ID: string;
    ExternalID: number;
}