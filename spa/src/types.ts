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

export interface InitData {
  statuses: Status[];
  requestTypes: RequestType[];
  user: { id: number; name: string; roleId: number };
}
