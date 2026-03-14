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


# Phase 2

## Critical Business Process

|Process|Description|System Component Involved|
|---|---|---|
|User Authentication & Onboarding|Students log in via GitHub OAuth. Admins manually register professors, triggering a mandatory password reset flow upon their first login.|Frontend, Backend, GitHub API, Database|
|Group & Advisor Formation|Students create groups (assigning a team leader), send advisee requests to professors, and professors approve or reject them. The system disbands groups without advisors.|Frontend, Backend, Database|
|Deliverable Submission|Team leaders submit a Proposal and Statement of Work (SoW) strictly within a coordinator-defined schedule using a WYSIWYG Markdown editor.|Frontend, Backend, Database|
|Grading & Assessment|Committee members review documents, leave comments, and assign binary or soft grades based on rubrics. The system calculates individual student grades based on completed vs. targeted story points.|Frontend, Backend, Database|
|Daily AI Sync & Audit|The system runs a daily synchronization with GitHub and JIRA. AI modules verify code reviews via PR comments and validate file diffs against JIRA issue descriptions.|Backend, AI Module, GitHub API, JIRA API|
|Committee Assignment|The coordinator assigns professors to evaluation committees responsible for reviewing proposals, SoW documents, and grading deliverables.|Frontend, Backend, Database|

---
---
---

## User Authentication & Onboarding

|Process Step|System Component|Data Required|
|---|---|---|
|Student clicks GitHub login|Frontend|OAuth request|
|System redirects to GitHub OAuth|Backend + GitHub API|Client ID, redirect URI|
|GitHub returns user profile|Backend|OAuth token, GitHub profile|
|Admin registers professor account|Admin Panel + Backend|Professor email, role|
|Professor first login password reset|Frontend + Backend|User ID, reset token|
|System stores user data|Backend + Database|User profile, role|

## Group & Advisor Formation

|Process Step|System Component|Data Required|
|---|---|---|
|Student creates group|Frontend + Backend|Student ID, Group name|
|System assigns team leader|Backend|Group ID, Student ID|
|Leader sends advisor request|Frontend + Backend|Group ID, Professor ID|
|Professor approves/rejects request|Frontend + Backend|Request ID, decision|
|Coordinator manages committees|Frontend + Backend|Advisor IDs, committee ID|
|System removes groups without advisor|Backend Job|Group ID|

## Deliverable Submission

|Process Step|System Component|Data Required|
|---|---|---|
|Team leader opens submission page|Frontend|Group ID|
|User writes document in Markdown editor|Frontend|Document content|
|System checks submission schedule|Backend|Current time, schedule window|
|Group submits proposal|Frontend + Backend|Group ID, document file|
|Group submits SoW|Frontend + Backend|Group ID, document file|
|System saves submission|Backend + Database|Document metadata|

## Grading & Assessment

|Process Step|System Component|Data Required|
|---|---|---|
|Committee opens submitted document|Frontend + Backend|Document ID|
|Reviewer adds comment|Frontend + Backend|Document ID, comment|
|Reviewer assigns grade|Frontend + Backend|Document ID, grade|
|Coordinator sets sprint requirements|Frontend + Backend|Sprint ID, target points|
|System calculates individual grade|Backend|Completed points, target points|
|System stores grade results|Backend + Database|Student ID, final grade|

## Daily AI Sync & Audit

|Process Step|System Component|Data Required|
|---|---|---|
|Scheduler triggers daily sync|Backend Scheduler|Timestamp|
|System fetches JIRA issues|Backend + JIRA API|Project ID|
|System fetches GitHub PR data|Backend + GitHub API|Repository ID|
|AI analyzes PR comments|AI Module|PR comment data|
|AI analyzes code diffs|AI Module|PR diff files|
|System stores audit results|Backend + Database|Analysis results|

## Committee Assignment

|Process Step|System Component|Data Required|
|---|---|---|
|Coordinator opens committee management page|Frontend|Coordinator ID|
|System retrieves available professors|Backend + Database|Professor list|
|Coordinator creates new committee|Frontend + Backend|Committee name, semester|
|Coordinator assigns professors to committee|Frontend + Backend|Professor ID, Committee ID|
|System stores committee membership|Backend + Database|Committee ID, Professor IDs|
|System links committee to student groups or submissions|Backend + Database|Committee ID, Group ID / Submission ID|
