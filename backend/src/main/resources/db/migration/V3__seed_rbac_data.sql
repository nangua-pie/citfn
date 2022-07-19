-- Seed the built in roles, permissions, role grants and starter boards.
--
-- RoleCodes and PermissionCodes in the Java sources describe exactly these
-- rows, so inserting them here keeps the constants and the database in step.
-- Ids are fixed so the grants below stay readable.

INSERT INTO sys_role (id, code, name, description) VALUES
    (1001, 'ROLE_USER',      'Member',       'A registered forum member'),
    (1002, 'ROLE_MODERATOR', 'Moderator',    'Can moderate posts and comments'),
    (1003, 'ROLE_ADMIN',     'Administrator', 'Full administrative access')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description);

INSERT INTO sys_permission (id, code, name, module, description) VALUES
    (2001, 'post:top',            'Pin posts',               'post',      'Pin a post to the top of its board'),
    (2002, 'post:essential',      'Mark posts essential',    'post',      'Mark a post as essential reading'),
    (2003, 'post:edit:any',       'Edit any post',           'post',      'Edit a post authored by someone else'),
    (2004, 'post:delete:any',     'Delete any post',         'post',      'Delete a post authored by someone else'),
    (2005, 'post:audit',          'Audit posts',             'post',      'Approve or reject a post awaiting review'),
    (2011, 'comment:edit:any',    'Edit any comment',        'comment',   'Edit a comment authored by someone else'),
    (2012, 'comment:delete:any',  'Delete any comment',      'comment',   'Delete a comment authored by someone else'),
    (2021, 'report:handle',       'Handle reports',          'report',    'Accept or dismiss a submitted report'),
    (2031, 'user:mute',          'Mute accounts',           'user',      'Temporarily mute an account'),
    (2032, 'user:ban',           'Ban accounts',            'user',      'Ban an account'),
    (2033, 'user:role:assign',   'Assign roles',            'user',      'Grant or revoke a role for a user'),
    (2034, 'user:im:ban',        'Ban private messaging',   'user',      'Disable private messaging for an account'),
    (2041, 'sensitive:manage',   'Manage sensitive words',  'sensitive', 'Add, edit and remove sensitive words'),
    (2042, 'category:manage',    'Manage boards',           'category',  'Create, edit and remove boards'),
    (2043, 'log:view',           'View operation logs',     'log',       'Read the administrator audit trail')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    module = VALUES(module),
    description = VALUES(description);

-- ROLE_USER grants nothing extra: signing in is what membership means. The
-- row is still assigned at registration so later grants have a place to land.

-- Moderators handle content but not system settings.
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
    (3101, 1002, 2001),
    (3102, 1002, 2002),
    (3103, 1002, 2003),
    (3104, 1002, 2004),
    (3105, 1002, 2005),
    (3106, 1002, 2011),
    (3107, 1002, 2012),
    (3108, 1002, 2021)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Administrators hold every permission, including any added later.
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 3200 + p.id, 1003, p.id FROM sys_permission p
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO forum_category (id, name, description, icon, sort_order, post_count, status, is_deleted) VALUES
    (5001, 'General',     'Anything that does not fit elsewhere', 'mdi-forum-outline', 10, 0, 1, 0),
    (5002, 'Development', 'Programming and tooling',              'mdi-code-tags',     20, 0, 1, 0),
    (5003, 'Help',        'Questions and answers',                'mdi-lifebuoy',      30, 0, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);
