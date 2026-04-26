export interface Card {
  id: number;
  documentNo: string;
  summary: string;
  statusId: number;
  statusName: string;
  priority: string;
  dueType: string;
  dateNextAction: number | null;
  salesRepId: number;
  salesRepName: string;
  bpartnerName: string;
  requestTypeName: string;
  lastMoveAt?: number;
}

export interface Status {
  id: number;
  name: string;
  seqNo: number;
  isClosed: boolean;
  isOpen: boolean;
  statusCategoryId: number;
}

export interface RequestType {
  id: number;
  name: string;
  statusCategoryId: number;
}

export interface Priority {
  value: string;
  name: string;
}

export interface SalesRep {
  id: number;
  name: string;
}

export interface BPartner {
  id: number;
  name: string;
}

export interface Project {
  id: number;
  name: string;
}

export interface InitData {
  statuses: Status[];
  requestTypes: RequestType[];
  priorities: Priority[];
  salesReps: SalesRep[];
  bpartners: BPartner[];
  projects: Project[];
  user: { id: number; name: string; roleId: number };
}
