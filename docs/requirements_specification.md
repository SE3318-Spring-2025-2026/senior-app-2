# Requirements Specification

## 1. Functional Requirements

### User Management and Authentication
- **FR1:** The system shall allow students to register and log in using their GitHub accounts via OAuth (e.g., NextAuth.js).
- **FR2.1:** The system shall allow administrators (Admin) to manually register professor accounts.
- **FR2.2:** The system shall force newly created professor accounts to be redirected to a password reset flow upon their initial login.
- **FR3:** The system shall be able to generate a one-time-use password reset link for administrators.

### Group, Advisor, and Committee Management
- **FR4:** The system shall allow students to create groups and must automatically appoint the student who created the group as the team leader for the first sprint.
- **FR5:** The system shall allow team leaders to send advisee requests to professors, and allow professors to either approve or reject these requests.
- **FR6:** The system shall allow coordinators to create committees and assign each advisor to these committees.
- **FR7:** The system shall automatically disband groups that do not have an assigned advisor (Sanitization).

### Deliverables and Grading
- **FR8:** The system shall allow groups (not individual students) to submit a Proposal and a Statement of Work (SoW) strictly within the active schedule bounds set by the coordinator, and shall explicitly reject any submission attempts made outside this timeframe.
- **FR9:** The system shall allow committee members to review submitted documents, leave comments, and grade them based on binary (S-100, F-0) or soft (A-100, B-80, etc.) rubric criteria.
- **FR10:** The system shall allow coordinators to set per-sprint story point requirements and configure the percentage contribution of each sprint to the deliverable grades.
- **FR11:** The system shall use the ratio of completed story points to the targeted story points to calculate each student's individual allowance from the overall deliverable grade.
- **FR12:** The system shall provide an embedded Markdown editor with WYSIWYG support, including the ability to insert images, for documents and rubric descriptions.

### Artificial Intelligence (AI) Modules
- **FR13:** The system shall run an AI module that reads Pull Request comments on GitHub to verify that a code review process has actually occurred.
- **FR14:** The system shall include an AI module to read file diffs introduced in a PR and validate them against the Issue descriptions on JIRA/GitHub to check implementation accuracy.

---

## 2. Non-Functional Requirements

- **NFR1 (Performance - Response Time):** The system's primary user interfaces (e.g., student and advisor dashboards) must render and become fully interactive within 2.0 seconds for 95% of user requests under normal network conditions.
- **NFR2 (Latency - AI Processing):** The maximum acceptable latency for completing background AI-based Pull Request and code diff analyses shall be strictly 3 minutes from the moment the synchronization is triggered.
- **NFR3 (Auditability & Data Retention):** The system must trace log 100% of critical user events (e.g., authentication, grading changes, document uploads) including exact timestamps and user IDs, with a query retrieval time of under 1 second for administrative security audits.
- **NFR4 (Fault Tolerance & Availability):** The system must maintain 99% availability for local read operations (such as viewing previously fetched grades or dashboard data) even when external third-party services (GitHub/JIRA APIs) experience complete timeouts or 5xx server errors.
- **NFR5 (Security Enforcement):** The system shall enforce Role-Based Access Control (RBAC) ensuring that any unauthorized data mutation attempt (e.g., a standard student attempting a Team Leader submission) is rejected and logged with a security response time of less than 500 milliseconds.

---

## 3. Integration Requirements

- **IR1:** The system shall integrate with the GitHub OAuth API via NextAuth.js (or a similar framework) to handle user authentication processes.
- **IR2:** The system shall integrate with the JIRA API to fetch active stories and tasks (including Issue Key, Work, Assignee, Reporter, Resolution, Created, Updated, Description) within team projects to synchronize data.
- **IR3:** The system shall integrate with the GitHub API to access bound team repositories, check branches corresponding to the teams' tasks (Issue Keys) on JIRA, and verify Pull Request merge statuses.
- **IR4:** The system shall automatically trigger these integrations once a day (daily) to update the story and task statuses from the teams' external platforms (JIRA/GitHub).
