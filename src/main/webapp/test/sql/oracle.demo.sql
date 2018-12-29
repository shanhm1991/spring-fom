-- Create table
create table DEMO
(
  id        VARCHAR2(256),
  name      VARCHAR2(256),
  source    VARCHAR2(256),
  filetype  VARCHAR2(256),
  importway VARCHAR2(256)
)
tablespace USERS
  pctfree 10
  initrans 1
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );
