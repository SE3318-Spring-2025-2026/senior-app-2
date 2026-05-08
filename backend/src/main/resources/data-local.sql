-- Sadece local ortam için geçerli örnek veriler
-- 22070006084 benzeri yapıda birkaç öğrenci numarası demo amaçlı eklenir.

INSERT INTO valid_student_ids (student_id, added_by, added_date)
SELECT '22070006080', 'System Admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM valid_student_ids WHERE student_id = '22070006080');

INSERT INTO valid_student_ids (student_id, added_by, added_date)
SELECT '22070006081', 'System Admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM valid_student_ids WHERE student_id = '22070006081');

INSERT INTO valid_student_ids (student_id, added_by, added_date)
SELECT '22070006082', 'System Admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM valid_student_ids WHERE student_id = '22070006082');

INSERT INTO valid_student_ids (student_id, added_by, added_date)
SELECT '22070006083', 'System Admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM valid_student_ids WHERE student_id = '22070006083');

INSERT INTO valid_student_ids (student_id, added_by, added_date)
SELECT '22070006084', 'System Admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM valid_student_ids WHERE student_id = '22070006084');

INSERT INTO valid_student_ids (student_id, added_by, added_date)
SELECT '22070006085', 'System Admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM valid_student_ids WHERE student_id = '22070006085');

INSERT INTO valid_student_ids (student_id, added_by, added_date)
SELECT '22070006086', 'System Admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM valid_student_ids WHERE student_id = '22070006086');


INSERT INTO users (email, password, full_name, role, enabled, status, created_at) SELECT 'coord1@example.com', '/zZ.qU5vXk5qO.QeH1kG7p7yQ/M9U3y4o/s4v2R8y6w0n5q', 'Coord One', 'COORDINATOR', true, 'ACTIVE', NOW() WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'coord1@example.com');
INSERT INTO users (email, password, full_name, role, enabled, status, created_at) SELECT 'prof1@example.com', '/zZ.qU5vXk5qO.QeH1kG7p7yQ/M9U3y4o/s4v2R8y6w0n5q', 'Prof One', 'PROFESSOR', true, 'ACTIVE', NOW() WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof1@example.com');

INSERT INTO users (email, password, full_name, role, enabled, status, created_at) SELECT 'prof2@example.com', '/zZ.qU5vXk5qO.QeH1kG7p7yQ/M9U3y4o/s4v2R8y6w0n5q', 'Prof Two', 'PROFESSOR', true, 'ACTIVE', NOW() WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof2@example.com');
INSERT INTO users (email, password, full_name, role, enabled, status, created_at) SELECT 'prof3@example.com', '/zZ.qU5vXk5qO.QeH1kG7p7yQ/M9U3y4o/s4v2R8y6w0n5q', 'Prof Three', 'PROFESSOR', true, 'ACTIVE', NOW() WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof3@example.com');
INSERT INTO users (email, password, full_name, role, enabled, status, created_at) SELECT 'prof4@example.com', '/zZ.qU5vXk5qO.QeH1kG7p7yQ/M9U3y4o/s4v2R8y6w0n5q', 'Prof Four', 'PROFESSOR', true, 'ACTIVE', NOW() WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof4@example.com');
INSERT INTO users (email, password, full_name, role, enabled, status, created_at) SELECT 'prof5@example.com', '/zZ.qU5vXk5qO.QeH1kG7p7yQ/M9U3y4o/s4v2R8y6w0n5q', 'Prof Five', 'PROFESSOR', true, 'ACTIVE', NOW() WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'prof5@example.com');
INSERT INTO users (email, password, full_name, role, enabled, status, created_at) SELECT 'coord2@example.com', '/zZ.qU5vXk5qO.QeH1kG7p7yQ/M9U3y4o/s4v2R8y6w0n5q', 'Coord Two', 'COORDINATOR', true, 'ACTIVE', NOW() WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'coord2@example.com');
