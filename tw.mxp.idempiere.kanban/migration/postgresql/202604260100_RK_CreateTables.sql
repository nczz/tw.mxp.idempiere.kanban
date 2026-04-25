SELECT register_migration_script('202604260100_RK_CreateTables.sql') FROM dual;

-- ============================================================
-- RK_Card_Member — Kanban card members
-- ============================================================
CREATE TABLE IF NOT EXISTS RK_Card_Member (
    RK_Card_Member_ID    NUMERIC(10) NOT NULL,
    RK_Card_Member_UU    VARCHAR(36) DEFAULT generate_uuid(),
    AD_Client_ID         NUMERIC(10) NOT NULL,
    AD_Org_ID            NUMERIC(10) NOT NULL DEFAULT 0,
    IsActive             CHAR(1) NOT NULL DEFAULT 'Y',
    Created              TIMESTAMP NOT NULL DEFAULT statement_timestamp(),
    CreatedBy            NUMERIC(10) NOT NULL,
    Updated              TIMESTAMP NOT NULL DEFAULT statement_timestamp(),
    UpdatedBy            NUMERIC(10) NOT NULL,
    R_Request_ID         NUMERIC(10) NOT NULL,
    AD_User_ID           NUMERIC(10) NOT NULL,
    MemberRole           VARCHAR(20) DEFAULT 'Observer',
    CONSTRAINT RK_Card_Member_Key PRIMARY KEY (RK_Card_Member_ID),
    CONSTRAINT RK_CardMember_Request FOREIGN KEY (R_Request_ID) REFERENCES R_Request(R_Request_ID),
    CONSTRAINT RK_CardMember_User FOREIGN KEY (AD_User_ID) REFERENCES AD_User(AD_User_ID)
);

CREATE UNIQUE INDEX IF NOT EXISTS RK_Card_Member_UQ
    ON RK_Card_Member(R_Request_ID, AD_User_ID) WHERE IsActive='Y';

-- ============================================================
-- RK_Card_Move_Log — Kanban card move history
-- ============================================================
CREATE TABLE IF NOT EXISTS RK_Card_Move_Log (
    RK_Card_Move_Log_ID  NUMERIC(10) NOT NULL,
    RK_Card_Move_Log_UU  VARCHAR(36) DEFAULT generate_uuid(),
    AD_Client_ID         NUMERIC(10) NOT NULL,
    AD_Org_ID            NUMERIC(10) NOT NULL DEFAULT 0,
    IsActive             CHAR(1) NOT NULL DEFAULT 'Y',
    Created              TIMESTAMP NOT NULL DEFAULT statement_timestamp(),
    CreatedBy            NUMERIC(10) NOT NULL,
    Updated              TIMESTAMP NOT NULL DEFAULT statement_timestamp(),
    UpdatedBy            NUMERIC(10) NOT NULL,
    R_Request_ID         NUMERIC(10) NOT NULL,
    R_Status_ID_From     NUMERIC(10),
    R_Status_ID_To       NUMERIC(10) NOT NULL,
    Note                 VARCHAR(2000),
    CONSTRAINT RK_Card_Move_Log_Key PRIMARY KEY (RK_Card_Move_Log_ID),
    CONSTRAINT RK_MoveLog_Request FOREIGN KEY (R_Request_ID) REFERENCES R_Request(R_Request_ID),
    CONSTRAINT RK_MoveLog_StatusFrom FOREIGN KEY (R_Status_ID_From) REFERENCES R_Status(R_Status_ID),
    CONSTRAINT RK_MoveLog_StatusTo FOREIGN KEY (R_Status_ID_To) REFERENCES R_Status(R_Status_ID)
);

-- ============================================================
-- RK_Request_Type_Config — Request type kanban configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS RK_Request_Type_Config (
    RK_Request_Type_Config_ID  NUMERIC(10) NOT NULL,
    RK_Request_Type_Config_UU  VARCHAR(36) DEFAULT generate_uuid(),
    AD_Client_ID               NUMERIC(10) NOT NULL,
    AD_Org_ID                  NUMERIC(10) NOT NULL DEFAULT 0,
    IsActive                   CHAR(1) NOT NULL DEFAULT 'Y',
    Created                    TIMESTAMP NOT NULL DEFAULT statement_timestamp(),
    CreatedBy                  NUMERIC(10) NOT NULL,
    Updated                    TIMESTAMP NOT NULL DEFAULT statement_timestamp(),
    UpdatedBy                  NUMERIC(10) NOT NULL,
    R_RequestType_ID           NUMERIC(10) NOT NULL,
    Default_AD_Role_ID         NUMERIC(10),
    Default_SalesRep_ID        NUMERIC(10),
    CONSTRAINT RK_Request_Type_Config_Key PRIMARY KEY (RK_Request_Type_Config_ID),
    CONSTRAINT RK_ReqTypeConfig_Type FOREIGN KEY (R_RequestType_ID) REFERENCES R_RequestType(R_RequestType_ID),
    CONSTRAINT RK_ReqTypeConfig_Role FOREIGN KEY (Default_AD_Role_ID) REFERENCES AD_Role(AD_Role_ID),
    CONSTRAINT RK_ReqTypeConfig_Rep FOREIGN KEY (Default_SalesRep_ID) REFERENCES AD_User(AD_User_ID)
);

CREATE UNIQUE INDEX IF NOT EXISTS RK_ReqTypeConfig_UQ
    ON RK_Request_Type_Config(R_RequestType_ID, AD_Client_ID);

-- ============================================================
-- Sequences for ID generation
-- ============================================================
INSERT INTO AD_Sequence (AD_Sequence_ID, Name, CurrentNext, IsAudited, StartNewYear, Description,
    IsActive, IsTableID, AD_Client_ID, AD_Org_ID, Created, CreatedBy, Updated, UpdatedBy,
    IsAutoSequence, StartNo, IncrementNo, CurrentNextSys, AD_Sequence_UU)
SELECT nextval('ad_sequence_sq'), 'RK_Card_Member', 1000000, 'N', 'N', 'Table RK_Card_Member',
    'Y', 'Y', 0, 0, statement_timestamp(), 100, statement_timestamp(), 100,
    'Y', 1000000, 1, 200000, generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Sequence WHERE Name='RK_Card_Member' AND IsTableID='Y');

INSERT INTO AD_Sequence (AD_Sequence_ID, Name, CurrentNext, IsAudited, StartNewYear, Description,
    IsActive, IsTableID, AD_Client_ID, AD_Org_ID, Created, CreatedBy, Updated, UpdatedBy,
    IsAutoSequence, StartNo, IncrementNo, CurrentNextSys, AD_Sequence_UU)
SELECT nextval('ad_sequence_sq'), 'RK_Card_Move_Log', 1000000, 'N', 'N', 'Table RK_Card_Move_Log',
    'Y', 'Y', 0, 0, statement_timestamp(), 100, statement_timestamp(), 100,
    'Y', 1000000, 1, 200000, generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Sequence WHERE Name='RK_Card_Move_Log' AND IsTableID='Y');

INSERT INTO AD_Sequence (AD_Sequence_ID, Name, CurrentNext, IsAudited, StartNewYear, Description,
    IsActive, IsTableID, AD_Client_ID, AD_Org_ID, Created, CreatedBy, Updated, UpdatedBy,
    IsAutoSequence, StartNo, IncrementNo, CurrentNextSys, AD_Sequence_UU)
SELECT nextval('ad_sequence_sq'), 'RK_Request_Type_Config', 1000000, 'N', 'N', 'Table RK_Request_Type_Config',
    'Y', 'Y', 0, 0, statement_timestamp(), 100, statement_timestamp(), 100,
    'Y', 1000000, 1, 200000, generate_uuid()
WHERE NOT EXISTS (SELECT 1 FROM AD_Sequence WHERE Name='RK_Request_Type_Config' AND IsTableID='Y');

-- ============================================================
-- AD_Form — Register the Kanban form
-- ============================================================
INSERT INTO AD_Form (AD_Form_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy,
    Updated, UpdatedBy, Name, Description, Classname, AccessLevel,
    IsBetaFunctionality, EntityType, AD_Form_UU)
SELECT nextval('ad_sequence_sq'), 0, 0, 'Y', statement_timestamp(), 100,
    statement_timestamp(), 100, 'Request Kanban', 'Kanban board for request management',
    'tw.mxp.idempiere.kanban.KanbanFormController', '3',
    'N', 'U', 'tw-idempiere-kanban-form-001'
WHERE NOT EXISTS (SELECT 1 FROM AD_Form WHERE AD_Form_UU = 'tw-idempiere-kanban-form-001');

-- AD_Form_Trl — translations for all system languages
INSERT INTO AD_Form_Trl (AD_Form_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive,
    Created, CreatedBy, Updated, UpdatedBy, Name, Description, Help, IsTranslated, AD_Form_Trl_UU)
SELECT f.AD_Form_ID, l.AD_Language, 0, 0, 'Y',
    statement_timestamp(), 100, statement_timestamp(), 100,
    f.Name, f.Description, f.Help, 'N', generate_uuid()
FROM AD_Form f, AD_Language l
WHERE f.AD_Form_UU = 'tw-idempiere-kanban-form-001'
  AND l.IsActive = 'Y' AND l.IsSystemLanguage = 'Y' AND l.IsBaseLanguage = 'N'
  AND NOT EXISTS (SELECT 1 FROM AD_Form_Trl t
                  WHERE t.AD_Form_ID = f.AD_Form_ID AND t.AD_Language = l.AD_Language);

-- ============================================================
-- AD_Menu — Register menu item
-- ============================================================
INSERT INTO AD_Menu (AD_Menu_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy,
    Updated, UpdatedBy, Name, Description, IsSummary, IsSOTrx, IsReadOnly,
    Action, AD_Form_ID, EntityType, AD_Menu_UU)
SELECT nextval('ad_sequence_sq'), 0, 0, 'Y', statement_timestamp(), 100,
    statement_timestamp(), 100, 'Request Kanban', 'Kanban board for request management',
    'N', 'N', 'N',
    'X', f.AD_Form_ID, 'U', 'tw-idempiere-kanban-menu-001'
FROM AD_Form f
WHERE f.AD_Form_UU = 'tw-idempiere-kanban-form-001'
  AND NOT EXISTS (SELECT 1 FROM AD_Menu WHERE AD_Menu_UU = 'tw-idempiere-kanban-menu-001');

-- AD_Menu_Trl — translations for all system languages
INSERT INTO AD_Menu_Trl (AD_Menu_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive,
    Created, CreatedBy, Updated, UpdatedBy, Name, Description, IsTranslated, AD_Menu_Trl_UU)
SELECT m.AD_Menu_ID, l.AD_Language, 0, 0, 'Y',
    statement_timestamp(), 100, statement_timestamp(), 100,
    m.Name, m.Description, 'N', generate_uuid()
FROM AD_Menu m, AD_Language l
WHERE m.AD_Menu_UU = 'tw-idempiere-kanban-menu-001'
  AND l.IsActive = 'Y' AND l.IsSystemLanguage = 'Y' AND l.IsBaseLanguage = 'N'
  AND NOT EXISTS (SELECT 1 FROM AD_Menu_Trl t
                  WHERE t.AD_Menu_ID = m.AD_Menu_ID AND t.AD_Language = l.AD_Language);

-- ============================================================
-- AD_TreeNodeMM — Place menu item under Request menu folder
-- Parent_ID = 500 is the "Request" menu folder in standard iDempiere
-- ============================================================
INSERT INTO AD_TreeNodeMM (AD_Tree_ID, Node_ID, AD_Client_ID, AD_Org_ID, IsActive,
    Created, CreatedBy, Updated, UpdatedBy, Parent_ID, SeqNo, AD_TreeNodeMM_UU)
SELECT 10, m.AD_Menu_ID, 0, 0, 'Y',
    statement_timestamp(), 100, statement_timestamp(), 100,
    500, 99, generate_uuid()
FROM AD_Menu m
WHERE m.AD_Menu_UU = 'tw-idempiere-kanban-menu-001'
  AND NOT EXISTS (SELECT 1 FROM AD_TreeNodeMM t
                  WHERE t.AD_Tree_ID = 10 AND t.Node_ID = m.AD_Menu_ID);

-- ============================================================
-- AD_Form_Access — Grant access to all existing roles
-- ============================================================
INSERT INTO AD_Form_Access (AD_Form_ID, AD_Role_ID, AD_Client_ID, AD_Org_ID, IsActive,
    Created, CreatedBy, Updated, UpdatedBy, IsReadWrite, AD_Form_Access_UU)
SELECT f.AD_Form_ID, r.AD_Role_ID, r.AD_Client_ID, 0, 'Y',
    statement_timestamp(), 100, statement_timestamp(), 100,
    'Y', generate_uuid()
FROM AD_Form f, AD_Role r
WHERE f.AD_Form_UU = 'tw-idempiere-kanban-form-001'
  AND r.IsActive = 'Y'
  AND NOT EXISTS (SELECT 1 FROM AD_Form_Access a
                  WHERE a.AD_Form_ID = f.AD_Form_ID AND a.AD_Role_ID = r.AD_Role_ID);
