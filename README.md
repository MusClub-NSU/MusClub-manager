# 🎶 MusClub Manager

**MusClub Manager** is a web application designed for the NSU Music Club to improve the efficiency of planning and organizing musical events.
The service consolidates scheduling, task management, resource allocation, and communication into a single system, making the organizational process more convenient than using multiple third-party tools.

The application is developed as a **Progressive Web App (PWA)**, ensuring accessibility on both desktop and mobile devices.

---

## 🚧 Possible Difficulties

* **Complex requirements:** the system must support a wide range of organizational tasks (from concert programs and timelines to sponsorship and promotion), which increases development complexity.
* **Balancing usability and functionality:** the platform must remain simple enough for students to use effectively while still providing advanced planning capabilities.
* **Notifications and deadlines:** implementing customizable reminders, deadline tracking, and escalation rules may be technically challenging.
* **Role-based access control:** differentiating between organizers and club members while keeping information flexible and secure could complicate the design.
* **User adoption:** transitioning from familiar tools (Trello, Google Sheets) may face resistance unless the new system proves significantly more convenient.

---

## ❓ Why This Project and How It Will Be Executed

Existing tools used by the NSU Music Club (Trello, Google Sheets, etc.) do not fully meet its organizational needs and often cause inefficiencies.

**MusClub Manager** will allow the club to:

* centralize event planning;
* assign responsibilities;
* track progress;
* notify participants;
* document every organizational step (venue preparation, content planning, sponsorship, promotion, etc.).

Thanks to its **PWA architecture**, the application will work seamlessly across devices and simplify distribution.

---

## 📌 Implementation Phases

1. **Requirements Analysis** — gather feedback from organizers and club members.
2. **Event Planning Module** — create events with descriptions, dates, venues, and programs. Support structured planning across mandatory steps (theme, venue, posters, promotions, sponsorship, etc.).
3. **Core Architecture and Data Model** — define a unified `Action` entity with properties. Implement role-based access (organizers vs. club members).
4. **Task and Timeline Management** — implement statuses (`not started`, `in progress`, `nearly finished`, `completed`, `canceled`, `failed`). Add customizable deadlines and reminder logic (for responsible users or the whole team).
5. **Testing and Iterative Feedback** — conduct user testing within the Music Club and adjust workflows and the interface based on feedback.

---

## 📱 Technologies

* **PWA** for cross-platform accessibility
* Role-based access model
* Notifications and reminders
* Structured event and task management

---

## 💡 Project Status

📌 Currently in the requirements and architecture design stage.

---

## 🚀 How to run

### Prerequisites

* Java 17+
* Docker & Docker Compose
* Free ports: **5432** (Postgres), **8080** (app)

### 1) Start PostgreSQL

From the project root:

```bash
docker compose up -d
docker compose ps   # should show musclub_pg: Up (healthy)
```

### 2) Run the app

```bash
./gradlew bootRun
```

Check the log line for correctness:

```
Tomcat started on port 8080 (http) with context path '/'
```

### 3) Verify API is up

Open Swagger UI:

* [http://localhost:8080/swagger](http://localhost:8080/swagger)

Or quick check:

```bash
curl -s http://localhost:8080/api/docs | head -n 5   # OpenAPI JSON
```

### 4) Quick CRUD smoke (it's not necessary)

```bash
# Create
curl -s -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@nsu.ru","role":"ORGANIZER"}'

# List
curl -s 'http://localhost:8080/api/users?size=10&page=0'
```
