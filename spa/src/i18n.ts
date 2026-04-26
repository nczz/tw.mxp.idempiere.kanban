/** All translatable message keys used in the SPA */
const DEFAULT_MESSAGES: Record<string, string> = {
  // Toolbar
  'KanbanPrivate': 'Private',
  'KanbanSubordinates': 'Subordinates',
  'KanbanAll': 'All',
  'KanbanAllTypes': 'All Types',
  'KanbanSearch': 'Search...',
  'KanbanNew': '+ New',
  'KanbanOpen': '📋 Open',
  'KanbanClosed': '📦 Closed',
  // Card
  'KanbanNoCards': 'No cards',
  'KanbanNoStatuses': 'No statuses configured.',
  'KanbanDaysAgo': 'd',
  // CardDetail
  'KanbanEdit': 'Edit',
  'KanbanSave': 'Save',
  'KanbanSaving': 'Saving...',
  'KanbanCancel': 'Cancel',
  'KanbanNotesResult': 'Notes / Result',
  'KanbanNoNotes': 'No notes',
  'KanbanERPLinks': 'ERP Links',
  'KanbanNoLinks': 'No linked records',
  'KanbanMoveHistory': 'Move History',
  'KanbanNoMoves': 'No moves recorded',
  'KanbanEscalated': 'Escalated',
  // NewCardDialog
  'KanbanNewRequest': 'New Request',
  'KanbanSummary': 'Summary',
  'KanbanCreate': 'Create',
  'KanbanCreating': 'Creating...',
  'KanbanSelectNone': '— Select —',
  'KanbanNone': '— None —',
  'KanbanAdditionalDetails': 'Additional details...',
  'KanbanDescribeRequest': 'Describe the request...',
  // Fields
  'KanbanStatus': 'Status',
  'KanbanRequestType': 'Request Type',
  'KanbanPriority': 'Priority',
  'KanbanSalesRep': 'Sales Rep',
  'KanbanRequester': 'Requester',
  'KanbanCreatedBy': 'Created By',
  'KanbanCreated': 'Created',
  'KanbanNextAction': 'Next Action',
  'KanbanStartDate': 'Start Date',
  'KanbanCloseDate': 'Close Date',
  'KanbanDateNextAction': 'Date Next Action',
  'KanbanBusinessPartner': 'Business Partner',
  'KanbanProduct': 'Product',
  'KanbanOrder': 'Order',
  'KanbanInvoice': 'Invoice',
  'KanbanPayment': 'Payment',
  'KanbanProject': 'Project',
  'KanbanCampaign': 'Campaign',
  'KanbanAsset': 'Asset',
  'KanbanActivity': 'Activity',
  // Errors
  'KanbanLoading': 'Loading...',
  'KanbanLoadingCards': 'Loading cards...',
  'KanbanFailedToLoad': 'Failed to load',
  'KanbanNoToken': 'No authentication token',
  'KanbanNoTokenHint': 'Please open this form from the iDempiere menu.',
  'KanbanMoveFailed': 'Move failed',
  'KanbanComments': 'Comments',
  'KanbanNoComments': 'No comments yet',
  'KanbanAddComment': 'Add comment...',
  'KanbanPost': 'Post',
  'KanbanPosting': 'Posting...',
  'KanbanAttachments': 'Attachments',
  'KanbanNoAttachments': 'No attachments',
  'KanbanUpload': 'Upload',
  'KanbanUploading': 'Uploading...',
  'KanbanDeleteConfirm': 'Delete this file?',
};

let messages: Record<string, string> = { ...DEFAULT_MESSAGES };

/** Initialize with server-provided translations (overrides defaults) */
export function setMessages(serverMessages: Record<string, string>) {
  messages = { ...DEFAULT_MESSAGES, ...serverMessages };
}

/** Get translated message by key */
export function t(key: string): string {
  return messages[key] || key;
}
