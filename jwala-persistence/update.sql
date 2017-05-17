-- ================================================== --
-- === BEGIN: UPGRADE FROM Jwala 0.0.1 to Jwala 1.3.0 === --
-- ================================================== --
-- Update the App table
ALTER TABLE APP ADD COLUMN IF NOT EXISTS loadBalanceAcrossServers BOOLEAN DEFAULT FALSE;
ALTER TABLE APP ADD COLUMN IF NOT EXISTS secure BOOLEAN DEFAULT FALSE;
ALTER TABLE APP ADD COLUMN IF NOT EXISTS unpackWar BOOLEAN DEFAULT FALSE;
ALTER TABLE APP ADD COLUMN IF NOT EXISTS warName VARCHAR(255);

-- Remove the old state and history tables
DROP TABLE IF EXISTS current_jvm_state;
DROP TABLE IF EXISTS current_state;
DROP TABLE IF EXISTS group_control_history;
DROP TABLE IF EXISTS jvm_control_history;
DROP TABLE IF EXISTS webserver_control_history;

-- Add and update the template tables
CREATE TABLE IF NOT EXISTS APP_CONFIG_TEMPLATE (ID INTEGER NOT NULL IDENTITY, createBy VARCHAR(255), createDate TIMESTAMP, lastUpdateDate TIMESTAMP, updateBy VARCHAR(255), locked BOOLEAN, metaData VARCHAR(2147483647), TEMPLATE_CONTENT VARCHAR(2147483647) NOT NULL, TEMPLATE_NAME VARCHAR(255) NOT NULL, APP_ID BIGINT, JVM_ID BIGINT, CONSTRAINT U_PP_CPLT_APP_ID UNIQUE (APP_ID, TEMPLATE_NAME, JVM_ID));
ALTER TABLE APP_CONFIG_TEMPLATE ADD COLUMN IF NOT EXISTS JVM_ID BIGINT;
ALTER TABLE APP_CONFIG_TEMPLATE DROP CONSTRAINT IF EXISTS U_PP_CPLT_APP_ID;
ALTER TABLE APP_CONFIG_TEMPLATE ADD CONSTRAINT IF NOT EXISTS U_PP_CPLT_APP_ID UNIQUE (APP_ID, TEMPLATE_NAME, JVM_ID);
CREATE TABLE IF NOT EXISTS GRP_APP_CONFIG_TEMPLATE (ID INTEGER NOT NULL IDENTITY, createBy VARCHAR(255), createDate TIMESTAMP, lastUpdateDate TIMESTAMP, updateBy VARCHAR(255), locked BOOLEAN, metaData VARCHAR(2147483647), TEMPLATE_CONTENT VARCHAR(2147483647) NOT NULL, TEMPLATE_NAME VARCHAR(255) NOT NULL, GRP_ID BIGINT, CONSTRAINT U_GRP_PLT_GRP_ID1 UNIQUE (GRP_ID, TEMPLATE_NAME));
CREATE TABLE IF NOT EXISTS GRP_JVM_CONFIG_TEMPLATE (ID INTEGER NOT NULL IDENTITY, createBy VARCHAR(255), createDate TIMESTAMP, lastUpdateDate TIMESTAMP, updateBy VARCHAR(255), locked BOOLEAN, metaData VARCHAR(2147483647), TEMPLATE_CONTENT VARCHAR(2147483647) NOT NULL, TEMPLATE_NAME VARCHAR(255) NOT NULL, GRP_ID BIGINT, CONSTRAINT U_GRP_PLT_GRP_ID2 UNIQUE (GRP_ID, TEMPLATE_NAME));
CREATE TABLE IF NOT EXISTS GRP_WEBSERVER_CONFIG_TEMPLATE (ID INTEGER NOT NULL IDENTITY, createBy VARCHAR(255), createDate TIMESTAMP, lastUpdateDate TIMESTAMP, updateBy VARCHAR(255), locked BOOLEAN, metaData VARCHAR(2147483647), TEMPLATE_CONTENT VARCHAR(2147483647) NOT NULL, TEMPLATE_NAME VARCHAR(255) NOT NULL, GRP_ID BIGINT, CONSTRAINT U_GRP_PLT_GRP_ID UNIQUE (GRP_ID, TEMPLATE_NAME));
CREATE TABLE IF NOT EXISTS history (id INTEGER NOT NULL IDENTITY, createBy VARCHAR(255), createDate TIMESTAMP, lastUpdateDate TIMESTAMP, updateBy VARCHAR(255), event VARCHAR(10000), EVENTTYPE VARCHAR(2), serverName VARCHAR(255) NOT NULL, groupId BIGINT, CONSTRAINT U_HISTORY_ID UNIQUE (id));

-- Add the updateby column to the grp table since it's extending the abstract persistence object
ALTER TABLE GRP ADD COLUMN IF NOT EXISTS updateBy VARCHAR(255);

-- Add the state attributes and other attributes to the JVM
ALTER TABLE JVM ADD COLUMN IF NOT EXISTS ERR_STS VARCHAR(2147483647);
ALTER TABLE JVM ADD COLUMN IF NOT EXISTS state VARCHAR(20) DEFAULT 'JVM_STARTED';
ALTER TABLE JVM ADD COLUMN IF NOT EXISTS statusPath VARCHAR(255) DEFAULT '/jwala.png';
ALTER TABLE JVM ADD COLUMN IF NOT EXISTS systemProperties VARCHAR(255);
ALTER TABLE JVM ADD COLUMN IF NOT EXISTS encryptedPassword VARCHAR(255);
ALTER TABLE JVM ADD COLUMN IF NOT EXISTS userName VARCHAR(255);

-- Add more template and resource tables
CREATE TABLE IF NOT EXISTS JVM_CONFIG_TEMPLATE (ID INTEGER NOT NULL IDENTITY, createBy VARCHAR(255), createDate TIMESTAMP, lastUpdateDate TIMESTAMP, updateBy VARCHAR(255), locked BOOLEAN, metaData VARCHAR(2147483647), TEMPLATE_CONTENT VARCHAR(2147483647) NOT NULL, TEMPLATE_NAME VARCHAR(255) NOT NULL, JVM_ID BIGINT, CONSTRAINT U_JVM_PLT_JVM_ID UNIQUE (JVM_ID, TEMPLATE_NAME));
CREATE TABLE IF NOT EXISTS RESOURCE_INSTANCE (RESOURCE_INSTANCE_ID INTEGER NOT NULL IDENTITY, createBy VARCHAR(255), createDate TIMESTAMP, lastUpdateDate TIMESTAMP, updateBy VARCHAR(255), name VARCHAR(255) NOT NULL, RESOURCE_INSTANCE_NAME VARCHAR(255) NOT NULL, RESOURCE_TYPE_NAME VARCHAR(255), GROUP_ID BIGINT, CONSTRAINT U_RSRCTNC_NAME UNIQUE (name), CONSTRAINT U_RSRCTNC_RESOURCE_INSTANCE_ID UNIQUE (RESOURCE_INSTANCE_ID, name, GROUP_ID));
CREATE TABLE IF NOT EXISTS RESOURCE_INSTANCE_ATTRIBUTES (RESOURCE_INSTANCE_ID BIGINT, ATTRIBUTE_KEY VARCHAR(255) NOT NULL, ATTRIBUTE_VALUE VARCHAR(255));

-- Add the state attributes and other attributes to the web server
ALTER TABLE WEBSERVER ADD COLUMN IF NOT EXISTS state VARCHAR(20) DEFAULT 'WS_UNREACHABLE';

-- Add another template table
CREATE TABLE IF NOT EXISTS WEBSERVER_CONFIG_TEMPLATE (ID INTEGER NOT NULL IDENTITY, createBy VARCHAR(255), createDate TIMESTAMP, lastUpdateDate TIMESTAMP, updateBy VARCHAR(255), locked BOOLEAN, metaData VARCHAR(2147483647), TEMPLATE_CONTENT VARCHAR(2147483647) NOT NULL, TEMPLATE_NAME VARCHAR(255) NOT NULL, WEBSERVER_ID BIGINT, CONSTRAINT U_WBSRPLT_WEBSERVER_ID UNIQUE (WEBSERVER_ID, TEMPLATE_NAME));

-- At some point the group ID and web server ID unique constraint was removed
ALTER TABLE WEBSERVER_GRP DROP CONSTRAINT IF EXISTS U_WBSRGRP_WEBSERVER_ID;

-- Add all the foreign keys
ALTER TABLE APP_CONFIG_TEMPLATE ADD CONSTRAINT IF NOT EXISTS FK_APP_ID_TO_APP__ID FOREIGN KEY (APP_ID) REFERENCES app (ID) ON DELETE CASCADE;
ALTER TABLE APP_CONFIG_TEMPLATE ADD CONSTRAINT IF NOT EXISTS FK_JVM_ID_TO_JVM__ID0 FOREIGN KEY (JVM_ID) REFERENCES jvm (id) ON DELETE CASCADE;
ALTER TABLE GRP_APP_CONFIG_TEMPLATE ADD CONSTRAINT IF NOT EXISTS FK_GRP_ID_TO_GRP__ID0 FOREIGN KEY (GRP_ID) REFERENCES grp (ID) ON DELETE CASCADE;
ALTER TABLE GRP_JVM_CONFIG_TEMPLATE ADD CONSTRAINT IF NOT EXISTS FK_GRP_ID_TO_GRP__ID1 FOREIGN KEY (GRP_ID) REFERENCES grp (ID) ON DELETE CASCADE;
ALTER TABLE GRP_WEBSERVER_CONFIG_TEMPLATE ADD CONSTRAINT IF NOT EXISTS FK_GRP_ID_TO_GRP__ID2 FOREIGN KEY (GRP_ID) REFERENCES grp (ID) ON DELETE CASCADE;
ALTER TABLE history ADD CONSTRAINT IF NOT EXISTS FK_GROUPID_TO_GRP__ID FOREIGN KEY (groupId) REFERENCES grp (ID);
ALTER TABLE JVM_CONFIG_TEMPLATE ADD CONSTRAINT IF NOT EXISTS FK_JVM_ID_TO_JVM__ID1 FOREIGN KEY (JVM_ID) REFERENCES jvm (id) ON DELETE CASCADE;
ALTER TABLE RESOURCE_INSTANCE ADD CONSTRAINT IF NOT EXISTS FK_GROUP_ID_TO_GRP__ID FOREIGN KEY (GROUP_ID) REFERENCES grp (ID);
ALTER TABLE RESOURCE_INSTANCE_ATTRIBUTES ADD CONSTRAINT IF NOT EXISTS FK_RESOURCE_INSTANCE_ID_TO_RESOURCE_INSTANCE__RESOURCE_INSTANCE_ID FOREIGN KEY (RESOURCE_INSTANCE_ID) REFERENCES RESOURCE_INSTANCE (RESOURCE_INSTANCE_ID);
ALTER TABLE WEBSERVER_CONFIG_TEMPLATE ADD CONSTRAINT IF NOT EXISTS FK_WEBSERVER_ID_TO_WEBSERVER__ID0 FOREIGN KEY (WEBSERVER_ID) REFERENCES webserver (id) ON DELETE CASCADE;
ALTER TABLE WEBSERVER_GRP ADD CONSTRAINT IF NOT EXISTS FK_WEBSERVER_ID_TO_WEBSERVER__ID1 FOREIGN KEY (WEBSERVER_ID) REFERENCES webserver (id);

CREATE TABLE IF NOT EXISTS VERSION (RELEASE_VERSION VARCHAR(255), LAST_UPDATED TIMESTAMP);
INSERT INTO VERSION (RELEASE_VERSION, LAST_UPDATED) VALUES ('1.3.0', NOW());
-- ================================================ --
-- === END: UPGRADE FROM Jwala 0.0.1 to Jwala 1.3.0 === --
-- ================================================ --


-- ================================================== --
-- === BEGIN: UPGRADE FROM Jwala 1.3.0 to Jwala 1.3.1 === --
-- ================================================== --
CREATE TABLE IF NOT EXISTS VERSION (RELEASE_VERSION VARCHAR(255), LAST_UPDATED TIMESTAMP);
INSERT INTO VERSION (RELEASE_VERSION, LAST_UPDATED) VALUES ('1.3.1', NOW());
-- ================================================ --
-- === END: UPGRADE FROM Jwala 1.3.0 to Jwala 1.3.1 === --
-- ================================================ --


-- ================================================== --
-- === BEGIN: UPGRADE FROM Jwala 1.3.1 to Jwala 1.3.2 === --
-- ================================================== --
-- Fix for JVM state set to null
UPDATE JVM SET STATE='JVM_STARTED' WHERE STATE IS NULL;

CREATE TABLE IF NOT EXISTS VERSION (RELEASE_VERSION VARCHAR(255), LAST_UPDATED TIMESTAMP);
INSERT INTO VERSION (RELEASE_VERSION, LAST_UPDATED) VALUES ('1.3.2', NOW());
-- ================================================ --
-- === END: UPGRADE FROM Jwala 1.3.1 to Jwala 1.3.2 === --
-- ================================================ --


-- =================================================== --
-- === BEGIN: UPGRADE FROM Jwala 1.3.2 to Jwala 1.3.10 === --
-- =================================================== --
CREATE TABLE IF NOT EXISTS RESOURCE_CONFIG_TEMPLATE (ID INTEGER NOT NULL IDENTITY, createBy VARCHAR(255), createDate TIMESTAMP, lastUpdateDate TIMESTAMP, updateBy VARCHAR(255), locked BOOLEAN, metaData VARCHAR(2147483647) NOT NULL, TEMPLATE_CONTENT VARCHAR(2147483647) NOT NULL, TEMPLATE_NAME VARCHAR(255) NOT NULL, APP_ID BIGINT, ENTITY_ID BIGINT, ENTITY_TYPE SMALLINT, GRP_ID BIGINT, CONSTRAINT U_RSRCPLT_ENTITY_ID UNIQUE (ENTITY_ID, APP_ID, GRP_ID, ENTITY_TYPE, TEMPLATE_NAME));
ALTER TABLE JVM ADD COLUMN IF NOT EXISTS encryptedPassword VARCHAR(255);

CREATE TABLE IF NOT EXISTS VERSION (RELEASE_VERSION VARCHAR(255), LAST_UPDATED TIMESTAMP);
INSERT INTO VERSION (RELEASE_VERSION, LAST_UPDATED) VALUES ('1.3.10', NOW());
-- ================================================= --
-- === END: UPGRADE FROM Jwala 1.3.2 to Jwala 1.3.10 === --
-- ================================================= --

-- ======================================================== --
-- === BEGIN: UPGRADE FROM Jwala 1.3.10 to Jwala 0.0.32 === --
-- ======================================================== --
ALTER TABLE JVM ADD COLUMN IF NOT EXISTS JDKMEDIA_ID BIGINT;
ALTER TABLE JVM ADD COLUMN IF NOT EXISTS TOMCATMEDIA_ID BIGINT;
INSERT INTO VERSION (RELEASE_VERSION, LAST_UPDATED) VALUES ('0.0.32', NOW());
-- ======================================================== --
-- === END : UPGRADE FROM Jwala 1.3.10 to Jwala 0.0.32 === --
-- ======================================================== --

-- =================================================== --
-- === BEGIN: UPGRADE FROM Jwala 1.3.10 to Jwala 0.0.38 === --
-- =================================================== --
ALTER TABLE history ALTER COLUMN event VARCHAR(100000);
INSERT INTO VERSION (RELEASE_VERSION, LAST_UPDATED) VALUES ('0.0.38', NOW());
-- ================================================= --
-- === END: UPGRADE FROM Jwala 1.3.10 to Jwala 0.0.38 === --
-- ================================================= --

-- =================================================== --
-- === BEGIN: UPGRADE FROM Jwala 0.0.38 to Jwala 0.0.185 === --
-- =================================================== --
ALTER TABLE WEBSERVER DROP COLUMN IF EXISTS SVRROOT;
ALTER TABLE WEBSERVER DROP COLUMN IF EXISTS DOCROOT;
ALTER TABLE WEBSERVER DROP COLUMN IF EXISTS ERR_STS;
ALTER TABLE WEBSERVER DROP COLUMN IF EXISTS HTTPCONFIGFILE;
INSERT INTO VERSION (RELEASE_VERSION, LAST_UPDATED) VALUES ('0.0.185', NOW());
-- =================================================== --
-- === END : UPGRADE FROM Jwala 0.0.38 to Jwala 0.0.185 === --
-- =================================================== --

-- =================================================== --
-- === BEGIN: UPGRADE FROM Jwala 0.0.38 to Jwala 0.0.216 === --
-- =================================================== --
ALTER TABLE media DROP CONSTRAINT U_MEDIA_NAME;
ALTER TABLE media ADD CONSTRAINT uq_media UNIQUE(NAME, TYPE);
-- =================================================== --
-- === END : UPGRADE FROM Jwala 0.0.38 to Jwala 0.0.216 === --
-- =================================================== --
