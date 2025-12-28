-- Drop all Flowable tables and sequences to ensure a clean start
-- This script will reset the Flowable database schema

-- Drop tables in the correct order to avoid foreign key constraints
DROP TABLE IF EXISTS act_hi_identitylink CASCADE;
DROP TABLE IF EXISTS act_hi_varinst CASCADE;
DROP TABLE IF EXISTS act_hi_taskinst CASCADE;
DROP TABLE IF EXISTS act_hi_actinst CASCADE;
DROP TABLE IF EXISTS act_hi_procinst CASCADE;
DROP TABLE IF EXISTS act_hi_detail CASCADE;
DROP TABLE IF EXISTS act_hi_comment CASCADE;
DROP TABLE IF EXISTS act_hi_attachment CASCADE;

DROP TABLE IF EXISTS act_ru_identitylink CASCADE;
DROP TABLE IF EXISTS act_ru_variable CASCADE;
DROP TABLE IF EXISTS act_ru_task CASCADE;
DROP TABLE IF EXISTS act_ru_execution CASCADE;
DROP TABLE IF EXISTS act_ru_job CASCADE;
DROP TABLE IF EXISTS act_ru_timer_job CASCADE;
DROP TABLE IF EXISTS act_ru_suspended_job CASCADE;
DROP TABLE IF EXISTS act_ru_deadletter_job CASCADE;
DROP TABLE IF EXISTS act_ru_event_subscr CASCADE;

DROP TABLE IF EXISTS act_re_procdef CASCADE;
DROP TABLE IF EXISTS act_re_model CASCADE;
DROP TABLE IF EXISTS act_re_deployment CASCADE;

DROP TABLE IF EXISTS act_ge_bytearray CASCADE;
DROP TABLE IF EXISTS act_ge_property CASCADE;

-- Drop CMMN tables
DROP TABLE IF EXISTS act_hi_caseinst CASCADE;
DROP TABLE IF EXISTS act_hi_mil_inst CASCADE;
DROP TABLE IF EXISTS act_ru_case_sentry_part CASCADE;
DROP TABLE IF EXISTS act_ru_mil_execution CASCADE;
DROP TABLE IF EXISTS act_ru_identitylink CASCADE;
DROP TABLE IF EXISTS act_ru_task CASCADE;
DROP TABLE IF EXISTS act_ru_variable CASCADE;
DROP TABLE IF EXISTS act_ru_plan_item_instance CASCADE;
DROP TABLE IF EXISTS act_ru_case_execution CASCADE;
DROP TABLE IF EXISTS act_re_case_def CASCADE;

-- Drop DMN tables
DROP TABLE IF EXISTS act_dmn_deployment CASCADE;
DROP TABLE IF EXISTS act_dmn_deployment_resource CASCADE;
DROP TABLE IF EXISTS act_dmn_hi_decision CASCADE;

-- Drop sequences
DROP SEQUENCE IF EXISTS act_evt_log_seq CASCADE;
DROP SEQUENCE IF EXISTS act_procdef_history_seq CASCADE;

-- Note: Application tables (claim_case, user, etc.) are preserved
-- If you want to drop them too, uncomment the following:

-- DROP TABLE IF EXISTS claim_document CASCADE;
-- DROP TABLE IF EXISTS claim_history CASCADE;
-- DROP TABLE IF EXISTS claim_case CASCADE;
-- DROP TABLE IF EXISTS insurance_policy CASCADE;
-- DROP TABLE IF EXISTS user_role CASCADE;
-- DROP TABLE IF EXISTS role CASCADE;
-- DROP TABLE IF EXISTS user_account CASCADE;
