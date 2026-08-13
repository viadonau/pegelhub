export interface StationDto {
  id: string;
  ownerId: string;
  stationNumber: string;
  name: string;
  waterBody: string;
  location?: string | null;
}

export interface StationOwnerDto {
  id: string;
  name: string;
  shortName?: string | null;
  notes?: string | null;
}
