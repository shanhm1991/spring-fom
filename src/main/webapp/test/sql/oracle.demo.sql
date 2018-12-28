--prompt Disabling triggers for DEMO...
alter table DEMO disable all triggers;
prompt Deleting DEMO...
delete from DEMO;
commit;
prompt Loading DEMO...
insert into DEMO (id, name, age, tel)
values ('1', '2', '3', '4');
insert into DEMO (id, name, age, tel)
values ('1', '2', '3', '4');
insert into DEMO (id, name, age, tel)
values ('1', '2', '3', '4');
commit;
prompt 3 records loaded
prompt Enabling triggers for DEMO...
alter table DEMO enable all triggers;
