# Getting Started

A guide for team members to get the project running locally.

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| **Node.js** | 18+ | [nodejs.org](https://nodejs.org/) or `brew install node` |
| **Java (OpenJDK)** | 21 | `brew install openjdk@21` |
| **Maven** | 3.9+ | `brew install maven` |
| **Git** | any | `brew install git` |

After installing Java via Homebrew, set `JAVA_HOME` so Maven can find it:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

To make this permanent, add the line above to your `~/.zshrc` (or `~/.bashrc`), then restart your terminal or run `source ~/.zshrc`.

---

## Clone the Repository

```bash
git clone https://github.com/SE3318-Spring-2025-2026/senior-app-2.git
cd senior-app-2
```

---

## Backend (Spring Boot)

First, set up a local mysql database. Then create your local application-local.properties file, in which you override the `spring.datasource.password=${MYSQL_PASSWORD:}` line with the password you used when setting up your database. You will probably also want to switch the JPA mode to create-delete while developing. Then:

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The API server starts at **http://localhost:8080**.

To verify it's running:

```bash
curl http://localhost:8080
```

You should get a response (a Whitelabel error page is normal — it means Tomcat is alive but no endpoints are defined yet).

### Useful Commands

| Command | Description |
|---------|-------------|
| `mvn clean install` | Clean build + run tests |
| `mvn spring-boot:run` | Start the server |
| `mvn test` | Run tests only |

---

## Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

The app starts at **http://localhost:5173**.

### Useful Commands

| Command | Description |
|---------|-------------|
| `npm install` | Install dependencies |
| `npm run dev` | Start dev server with hot reload |
| `npm run build` | Production build to `dist/` |
| `npm run preview` | Preview the production build locally |

---

## Running Both Together

Open two terminal windows/tabs:

**Terminal 1 — Backend:**
```bash
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
mvn spring-boot:run
```

**Terminal 2 — Frontend:**
```bash
cd frontend
npm run dev
```

---

## Project Structure

```
senior-app-2/
├── backend/                # Spring Boot API (Java 21, Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/seniorapp/
│       │   └── resources/application.properties
│       └── test/
├── frontend/               # React app (Vite)
│   ├── package.json
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       └── pages/
│           ├── Login.jsx
│           └── Panel.jsx
└── docs/                   # Documentation
```

---

## Test Accounts

The backend automatically seeds these accounts on first run:

| Role | Email | Password |
|------|-------|----------|
| **Admin** | `admin@seniorapp.com` | `admin123` |
| **Professor** | `professor@seniorapp.com` | `prof123` |
| **Student** | `student@seniorapp.com` | `student123` |

Staff accounts (Admin, Professor) log in via the **Staff** tab on the login page using email and password.

---

## Troubleshooting

### `mvn: command not found`
Maven is not installed or not in your PATH. Run `brew install maven`.

### `No plugin found for prefix 'spring-boot'`
You're not in the `backend/` directory. Make sure to `cd backend` first.

### `JAVA_HOME is not set`
Export it before running Maven:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

### Port already in use
Kill the process on the port:
```bash
lsof -i :8080 | grep LISTEN    # find the PID
kill <PID>                      # kill it
```
