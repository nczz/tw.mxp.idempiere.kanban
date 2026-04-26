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
  bpartnerId: number;
  bpartnerName: string;
  projectId: number;
  projectName: string;
  requestTypeName: string;
  orgName: string;
  lastMoveAt?: number;
  isEscalated?: boolean;
}

export interface Status {
  id: number;
  name: string;
  seqNo: number;
  isClosed: boolean;
  isOpen: boolean;
  isFinalClose?: boolean;
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
  user: { id: number; name: string; roleId: number; orgId: number };
  orgs?: { id: number; name: string }[];
  messages?: Record<string, string>;
  wipLimits?: Record<string, number>;
  priorityColors?: Record<string, string>;
  activeRequestTypeId?: number;
}
