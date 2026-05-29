-- V8__drop_exit_confirm_log.sql : ExitConfirm 기능 제거에 따른 테이블 삭제
IF EXISTS (SELECT * FROM sysobjects WHERE name='exit_confirm_log' AND xtype='U')
DROP TABLE exit_confirm_log;
