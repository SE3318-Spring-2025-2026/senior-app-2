# Phase 2: Business Process Mapping

## 1. Critical Business Process Overview

| Process | Description | System Component Involved |
|---|---|---|
| **User Authentication & Onboarding** | Coordinators upload valid student IDs. Students log in via GitHub OAuth. Admins manually register professors and explicitly generate a one-time-use password reset link for their first login. | Frontend, Backend, GitHub API, Database |
| **Group & Advisor Formation** | Students create groups (assigning a team leader) and the leader sets up the JIRA/GitHub integration using a Personal Access Token (PAT). Leaders send advisee requests to professors, who approve or reject them. The system disbands groups without advisors. | Frontend, Backend, Database |
| **Deliverable Submission** | Team leaders of groups assigned to a committee submit a Proposal, Statement of Work (SoW), and Demonstration strictly within a coordinator-defined schedule using a WYSIWYG Markdown editor with image support. | Frontend, Backend, Database |
| **Grading & Assessment** | Advisors grade team sprint performance (Point A) and code review (Point B). Committee members review submitted documents and assign rubric-based grades. The system calculates individual student grades based on completed vs. targeted story points and team allowances. | Frontend, Backend, Database |
| **Daily AI Sync & Audit** | The system runs a daily synchronization with GitHub and JIRA using team tokens. AI modules verify code reviews via PR comments and validate file diffs against JIRA issue descriptions, storing the results. | Backend, AI Module, GitHub API, JIRA API |
| **Committee Assignment** | The coordinator creates evaluation committees, assigns professors to them, and links these committees to student groups to enable submissions and grading. | Frontend, Backend, Database |

---

## 2. Process Details

### 2.1. User Authentication & Onboarding

| Process Step | System Component | Data Required |
|---|---|---|
| Coordinator uploads valid student IDs | Backend + Database | Student IDs (List) |
| Student initiates GitHub login | Frontend | OAuth Request |
| System redirects to GitHub OAuth | Backend + GitHub API | Client ID, Redirect URI |
| GitHub returns user profile | Backend | OAuth Token, GitHub Username |
| Admin registers professor account | Admin Panel + Backend | Professor Email, Role |
| Admin generates password reset link | Backend | One-time-use Link Request |
| Professor first login & reset | Frontend + Backend | Reset Token, New Password |
| System stores user data | Backend + Database | User Profile, Role, Status |

### 2.2. Group & Advisor Formation

| Process Step | System Component | Data Required |
|---|---|---|
| Student creates group | Frontend + Backend | Student ID, Group Name |
| System assigns team leader | Backend | Group ID, Student ID |
| Team leader sets up JIRA/GitHub Integration | Frontend + Backend | GitHub PAT, JIRA Space URL |
| Leader sends advisor request | Frontend + Backend | Group ID, Professor ID |
| Professor approves/rejects request | Frontend + Backend | Request ID, Decision (Approve/Reject) |
| System disbands groups without advisor | Backend Job | Group ID, Advisor Status |

### 2.3. Deliverable Submission

| Process Step | System Component | Data Required |
|---|---|---|
| Team leader opens submission page | Frontend | Group ID, Committee ID |
| User writes document in Markdown editor | Frontend | Markdown Content, Image Files |
| System checks submission schedule | Backend | Current Timestamp, Schedule Window |
| Group submits Proposal | Frontend + Backend | Group ID, Proposal Data |
| Group submits SoW | Frontend + Backend | Group ID, SoW Data |
| Group submits Demonstration | Frontend + Backend | Group ID, Demonstration Files/Links |
| System saves submission | Backend + Database | Document Metadata, Content |

### 2.4. Grading & Assessment

| Process Step | System Component | Data Required |
|---|---|---|
| Advisor grades team Scrum performance (**Point A**) | Frontend + Backend | Group ID, Soft Grade (A-F) |
| Advisor grades Work/Code Review (**Point B**) | Frontend + Backend | Group ID, Soft Grade (A-F) |
| Committee opens submitted document | Frontend + Backend | Document ID |
| Committee assigns rubric grades to deliverables | Frontend + Backend | Document ID, Rubric Selections |
| Coordinator sets sprint requirements & weights | Frontend + Backend | Sprint ID, Target Points, Weight % |
| System calculates individual allowance | Backend | Completed Points, Point A & B Average |
| System stores final individual grades | Backend + Database | Student ID, Calculated Final Grade |

### 2.5. Daily AI Sync & Audit

| Process Step | System Component | Data Required |
|---|---|---|
| Scheduler triggers daily sync | Backend Scheduler | Current Timestamp |
| System fetches JIRA issues using tokens | Backend + JIRA API | Project ID, Active Stories |
| System fetches GitHub PR data using PAT | Backend + GitHub API | Repository ID, Branch/PR Data |
| AI analyzes PR comments for review verification | AI Module | PR Comment Logs |
| AI analyzes code diffs against issue descriptions | AI Module | PR Diff Files, JIRA Issue Desc. |
| System stores AI audit results | Backend + Database | Analysis Summary, Accuracy Score |

### 2.6. Committee Assignment

| Process Step | System Component | Data Required |
|---|---|---|
| Coordinator opens management page | Frontend | Coordinator ID |
| System retrieves available professors | Backend + Database | Professor List |
| Coordinator creates new committee | Frontend + Backend | Committee Name, Semester |
| Coordinator assigns professors to committee | Frontend + Backend | Professor ID, Committee ID |
| System links committee to student groups | Backend + Database | Committee ID, Group ID |
| System stores committee membership | Backend + Database | Committee ID, Professor IDs |
