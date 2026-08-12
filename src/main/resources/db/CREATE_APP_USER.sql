-- ══════════════════════════════════════════════════════════════════════════
-- Oracle Stored Procedure: CREATE_APP_USER
-- Creates a new user in the APP_USER table.
--
-- Usage:
--   EXEC CREATE_APP_USER('john', 'password123', 'John Doe', 'DEVELOPER');
--   EXEC CREATE_APP_USER('jane', 'pass456', 'Jane Smith', 'DEPLOYER');
--   EXEC CREATE_APP_USER('boss', 'admin1', 'Boss Man', 'ADMIN');
--
-- Roles: DEVELOPER, DEPLOYER, ADMIN
--   DEVELOPER - can only access the Deployment Request tab
--   DEPLOYER  - can access all tabs (Single Upload, Bulk Upload, Deployment Request)
--   ADMIN     - can access all tabs + manage users
-- ══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE PROCEDURE CREATE_APP_USER (
    p_username     IN VARCHAR2,
    p_password     IN VARCHAR2,
    p_display_name IN VARCHAR2,
    p_role         IN VARCHAR2 DEFAULT 'DEVELOPER'
) AS
    v_count NUMBER;
    v_next_id NUMBER;
BEGIN
    -- Check if username already exists
    SELECT COUNT(*) INTO v_count
    FROM APP_USER
    WHERE USERNAME = UPPER(p_username);

    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'User "' || p_username || '" already exists.');
    END IF;

    -- Validate role
    IF UPPER(p_role) NOT IN ('DEVELOPER', 'DEPLOYER', 'ADMIN') THEN
        RAISE_APPLICATION_ERROR(-20002, 'Invalid role. Must be DEVELOPER, DEPLOYER, or ADMIN.');
    END IF;

    -- Get next sequence value
    SELECT APP_USER_SEQ.NEXTVAL INTO v_next_id FROM DUAL;

    -- Insert the new user
    INSERT INTO APP_USER (ID, USERNAME, PASSWORD, DISPLAY_NAME, ROLE, ACTIVE)
    VALUES (v_next_id, p_username, p_password, p_display_name, UPPER(p_role), 1);

    COMMIT;

    DBMS_OUTPUT.PUT_LINE('User "' || p_username || '" created successfully with role ' || UPPER(p_role));
END CREATE_APP_USER;
/

-- ══════════════════════════════════════════════════════════════════════════
-- Optional: Procedure to change password
-- Usage: EXEC CHANGE_USER_PASSWORD('john', 'newpassword');
-- ══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE PROCEDURE CHANGE_USER_PASSWORD (
    p_username     IN VARCHAR2,
    p_new_password IN VARCHAR2
) AS
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM APP_USER
    WHERE USERNAME = p_username AND ACTIVE = 1;

    IF v_count = 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'User "' || p_username || '" not found or is inactive.');
    END IF;

    UPDATE APP_USER
    SET PASSWORD = p_new_password
    WHERE USERNAME = p_username;

    COMMIT;

    DBMS_OUTPUT.PUT_LINE('Password changed for user "' || p_username || '".');
END CHANGE_USER_PASSWORD;
/

-- ══════════════════════════════════════════════════════════════════════════
-- Optional: Procedure to deactivate a user
-- Usage: EXEC DEACTIVATE_USER('john');
-- ══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE PROCEDURE DEACTIVATE_USER (
    p_username IN VARCHAR2
) AS
BEGIN
    UPDATE APP_USER
    SET ACTIVE = 0
    WHERE USERNAME = p_username;

    IF SQL%ROWCOUNT = 0 THEN
        RAISE_APPLICATION_ERROR(-20004, 'User "' || p_username || '" not found.');
    END IF;

    COMMIT;

    DBMS_OUTPUT.PUT_LINE('User "' || p_username || '" has been deactivated.');
END DEACTIVATE_USER;
/

-- ══════════════════════════════════════════════════════════════════════════
-- Sample: Create initial users (run once after deployment)
-- ══════════════════════════════════════════════════════════════════════════
-- EXEC CREATE_APP_USER('admin',     'admin123',   'Administrator',  'ADMIN');
-- EXEC CREATE_APP_USER('developer', 'dev123',     'Developer User', 'DEVELOPER');
-- EXEC CREATE_APP_USER('deployer',  'deploy123',  'Deployer User',  'DEPLOYER');
