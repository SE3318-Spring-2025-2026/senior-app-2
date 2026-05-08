-- Extra local sample data for template committee management checks.

INSERT INTO users (email, password, full_name, role, enabled, status, created_at)
SELECT 'coord.committee.local@seniorapp.com',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiV4J6yIhXCTITVEWilQoNqT9XyM54.',
       'Committee Coordinator Local',
       'COORDINATOR',
       TRUE,
       'ACTIVE',
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'coord.committee.local@seniorapp.com'
);

INSERT INTO users (email, password, full_name, role, enabled, status, created_at)
SELECT 'prof.committee.local@seniorapp.com',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiV4J6yIhXCTITVEWilQoNqT9XyM54.',
       'Committee Professor Local',
       'PROFESSOR',
       TRUE,
       'ACTIVE',
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'prof.committee.local@seniorapp.com'
);

INSERT INTO users (email, password, full_name, role, enabled, status, created_at)
SELECT '22070006084@seniorapp.com',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiV4J6yIhXCTITVEWilQoNqT9XyM54.',
       '22070006084 Sample Student',
       'STUDENT',
       TRUE,
       'ACTIVE',
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = '22070006084@seniorapp.com'
);

INSERT INTO project_templates (
    name, description, term, created_by, created_by_user_id,
    project_start_date, version, active, template_json, created_at, updated_at
)
SELECT 'Local Committee Demo Template',
       'Local sample for coordinator/professor assignment flow',
       'Spring 2026',
       'coord.committee.local@seniorapp.com',
       u.id,
       '2026-02-10',
       1,
       TRUE,
       '{"name":"Local Committee Demo Template","description":"Local sample for coordinator/professor assignment flow","term":"Spring 2026","projectStartDate":"2026-02-10","sprints":[]}',
       NOW(),
       NOW()
FROM users u
WHERE u.email = 'coord.committee.local@seniorapp.com'
  AND NOT EXISTS (
      SELECT 1 FROM project_templates WHERE name = 'Local Committee Demo Template'
  );

INSERT INTO template_committees (template_id, name, created_at, updated_at)
SELECT pt.id, 'Demo Committee A', NOW(), NOW()
FROM project_templates pt
WHERE pt.name = 'Local Committee Demo Template'
  AND NOT EXISTS (
      SELECT 1 FROM template_committees tc
      WHERE tc.template_id = pt.id AND tc.name = 'Demo Committee A'
  );

INSERT INTO template_committees (template_id, name, created_at, updated_at)
SELECT pt.id, 'Demo Committee B', NOW(), NOW()
FROM project_templates pt
WHERE pt.name = 'Local Committee Demo Template'
  AND NOT EXISTS (
      SELECT 1 FROM template_committees tc
      WHERE tc.template_id = pt.id AND tc.name = 'Demo Committee B'
  );

INSERT INTO template_committee_professors (committee_id, professor_user_id, created_at)
SELECT tc.id, u.id, NOW()
FROM template_committees tc
JOIN project_templates pt ON pt.id = tc.template_id
JOIN users u ON u.email = 'coord.committee.local@seniorapp.com'
WHERE pt.name = 'Local Committee Demo Template'
  AND tc.name = 'Demo Committee A'
  AND NOT EXISTS (
      SELECT 1 FROM template_committee_professors tcp
      WHERE tcp.committee_id = tc.id AND tcp.professor_user_id = u.id
  );

INSERT INTO template_committee_professors (committee_id, professor_user_id, created_at)
SELECT tc.id, u.id, NOW()
FROM template_committees tc
JOIN project_templates pt ON pt.id = tc.template_id
JOIN users u ON u.email = 'prof.committee.local@seniorapp.com'
WHERE pt.name = 'Local Committee Demo Template'
  AND tc.name = 'Demo Committee A'
  AND NOT EXISTS (
      SELECT 1 FROM template_committee_professors tcp
      WHERE tcp.committee_id = tc.id AND tcp.professor_user_id = u.id
  );
