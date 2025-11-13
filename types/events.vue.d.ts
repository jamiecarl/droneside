export interface ChannelType {
    ID: string;
    Number: number;
    Band: string;
    ChannelPrefix: string;
    Frequency: number;
    DisplayName: string | null;
    ExternalID: number;
    ShortBand: string;
    Color: string
}

export interface ClubType {
    ID: string;
    Creation: string;
    Name: string;
    Visible: true;
    LogoUrl: string;
    BannerUrl: string
    PrimaryColor: string;
    TextColor: string;
    Address: string;
    Latitude: number;
    Longitude: number;
    Timezone: string
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
    PilotName: string;
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
    Channel: ChannelType;
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

export interface PointsType {
    pilot_id: string;
    pilot: string;
    round: string;
    round_order: string;
    bracket: string;
    points: string;
}

export interface RaceSummaryType {
    pilot_id: string;
    holeshot_time: number;
    pb_lap_count: number;
    pb_lap_time: number;
    target_lap_count: number;
    target_lap_time: number;
    race_time: number;
    round_number: number;
    round_order: number;
    event_type: string;
}
