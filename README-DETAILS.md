# Student Matching App — Architecture and Operations Guide

This is a companion README that explains the project in depth: how it is structured, how it works internally, how to run it locally (with multiple container runtimes), and how to deploy it.

If you prefer a quick-start, see the main README. This document aims to be detailed and beginner-friendly.

Table of contents
- 1) Project goals and overview
- 2) Repository structure
- 3) Technology stack and versions
- 4) How the app works (end-to-end flow)
- 5) Domain model (entities and relationships)
- 6) Application layers and class roles
- 7) Security model (authentication and authorization)
- 8) Matching algorithm (current and future)
- 9) Configuration and environment variables
- 10) Database seeding (data.sql)
- 11) Running locally with containers (Colima, OrbStack, Docker Desktop, or Multipass)
- 12) Common commands (start/stop/reset/logs)
- 13) Troubleshooting guide
- 14) Deployment on a server
- 15) FAQ and next steps

---

1) Project goals and overview
- Purpose: A Student Portal for my School that matches student tutors with student tutees.
- Key features:
  - Register/login with school email.
  - Student profile with year group and exam board (for sixth form).
  - Create tutoring requests (offer or request help) by subject and available timeslots.
  - Weekly matching job pairs tutors and tutees (same subject, overlapping timeslot, year constraints).
  - Admin dashboard with system overview and basic alerts.

2) Repository structure
```
student-matching-app/
├── pom.xml
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── src/
│   ├── main/java/com/example/student_portal/
│   │   ├── StudentPortalApplication.java
│   │   ├── config/
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── AdminController.java
│   │   │   ├── AuthController.java
│   │   │   ├── MatchController.java
│   │   │   ├── ProfileController.java
│   │   │   └── RequestController.java
│   │   ├── entity/
│   │   │   ├── Match.java
│   │   │   ├── Request.java
│   │   │   ├── Subject.java
│   │   │   ├── Timeslot.java
│   │   │   └── User.java
│   │   ├── model/   (enums)
│   │   │   ├── ExamBoard.java
│   │   │   ├── RequestStatus.java
│   │   │   ├── RequestType.java
│   │   │   ├── Role.java
│   │   │   └── YearGroup.java
│   │   ├── repository/
│   │   │   ├── MatchRepository.java
│   │   │   ├── RequestRepository.java
│   │   │   ├── SubjectRepository.java
│   │   │   ├── TimeslotRepository.java
│   │   │   └── UserRepository.java
│   │   ├── service/
│   │   │   ├── MatchService.java
│   │   │   ├── RequestService.java
│   │   │   ├── SubjectService.java
│   │   │   ├── TimeslotService.java
│   │   │   ├── UserService.java
│   │   │   └── security/CustomUserDetailsService.java
│   │   └── util/
│   │       └── MatchingAlgorithm.java
│   └── main/resources/
│       ├── application.properties
│       ├── data.sql
│       └── templates/ + static/css/
└── README.md (main)
└── README-DETAILS.md (this file)
```

3) Technology stack and versions
- Language: Java 17
- Framework: Spring Boot 3.x
  - Spring MVC (web + Thymeleaf)
  - Spring Security 6 (auth)
  - Spring Data JPA (Hibernate)
  - Validation (Jakarta Validation)
  - Scheduling (for weekly match job)
- Database: PostgreSQL 15+
- Build: Maven
- Containerization: Docker images + Docker Compose
- Local runtimes:
  - Colima (recommended for macOS 12 users - me)
  - OrbStack (great alternative)
  - Docker Desktop (requires macOS 13+)
  - Multipass (Ubuntu VM) + Docker inside VM

4) How the app works (end-to-end flow)
- Registration:
  - User signs up at /register with an @bromsgrove-school.co.uk email.
  - Role determined: email starting with a digit → STUDENT; otherwise → ADMIN.
  - Password is hashed with BCrypt.
  - Auto-login after successful registration.
- Profile:
  - User sets their year group and exam board (IB/A_LEVELS for Year 12/13, otherwise NONE).
  - User can later select subjects and availability (timeslots) via future enhancements.
- Requests:
  - A student creates a Request as TUTOR (offer) or TUTEE (needs help) for a single Subject and multiple possible Timeslots.
  - Duplicate prevention: cannot have multiple outstanding requests for the same (user + subject + type).
- Matching:
  - Weekly job checks all outstanding TUTOR and TUTEE requests.
  - Pairs them when: subject matches, timeslots overlap, and tutor’s year ≥ tutee’s year.
  - Creates a Match entity with the assigned Timeslot and marks requests as MATCHED.
- Views:
  - /matches shows a user’s active matches.
  - /admin (admins only) shows users, outstanding requests, and current matches.

5) Domain model (entities and relationships)
- User
  - Fields: id, fullName, email, passwordHash, role (STUDENT/ADMIN), yearGroup, examBoard.
  - Relationships: many-to-many subjects, many-to-many availableTimeslots.
- Subject
  - Fields: id, name.
  - Relationships: inverse to users and requests.
- Timeslot
  - Fields: id, label ("Monday Period 3" etc.).
  - Relationships: inverse to users and requests.
- Request
  - Fields: id, user, subject, possibleTimeslots, type (TUTOR/TUTEE), status (OUTSTANDING, MATCHED, etc.), yearGroup (copied from user at creation).
  - Links one User to one Subject with multiple Timeslot options.
- Match
  - Fields: id, tutorRequest, tuteeRequest, matchedTimeslot, status (simple text like ACTIVE).
  - Represents a final pairing made by the matcher.

Relationship notes:
- User ↔ Subject: many-to-many (what they study/can tutor).
- User ↔ Timeslot: many-to-many (availability).
- Request ↔ Timeslot: many-to-many (possible meeting times).
- Match → Request: many-to-one (one tutor request, one tutee request).
- Match → Timeslot: many-to-one (assigned meeting slot).

6) Application layers and class roles
- Controllers (web endpoints + view orchestration; minimal logic)
  - AuthController: login/register pages and registration handling (auto-login after success).
  - ProfileController: shows/updates profile; supplies enums and lists to the view.
  - RequestController: shows user’s requests; handles creation/deletion; supplies subjects/timeslots/type list.
  - MatchController: shows matches for the logged-in user.
  - AdminController: admin-only dashboard with system overview and a basic tutor-availability alert.
- Services (business logic + transactions)
  - UserService: registration, hashing, role inference, find/save helpers.
  - RequestService: create with validation, duplicate prevention, fetch outstanding tutor/tutee, update status, delete.
  - MatchService: persist matches, mark requests MATCHED, find matches for user/all.
  - SubjectService/TimeslotService: simple wrappers around repositories.
  - security/CustomUserDetailsService: adapts User to Spring Security’s UserDetails (email as username).
- Repositories (data access)
  - UserRepository, SubjectRepository, TimeslotRepository, RequestRepository, MatchRepository.
  - Spring Data generates query implementations (e.g., findByEmail, findByTypeAndStatus).
- Utilities
  - MatchingAlgorithm: scheduled component that runs the weekly matching.
- Configuration
  - SecurityConfig: HTTP security rules, login/logout, static resource ignoring, user details + encoder beans.
  - StudentPortalApplication: app entry point, enables scheduling (@EnableScheduling).

7) Security model (authentication and authorization)
- Authentication:
  - Username = email; password hashed with BCrypt.
  - CustomUserDetailsService loads the User from DB and maps role to authority "ROLE_STUDENT" or "ROLE_ADMIN".
- Authorization rules in SecurityConfig:
  - Public: "/", "/login", "/register", and static assets (/css, /js, /images).
  - Admin-only: "/admin/**".
  - All others: require authentication.
- Login/Logout:
  - Custom login page at /login; default success redirects to /profile.
  - Logout clears session and redirects to /login?logout.

8) Matching algorithm (current and future)
- Current approach: simple greedy
  - For each outstanding TUTOR request, scan TUTEE requests for a compatible candidate (same subject, overlapping timeslot, tutorYear ≥ tuteeYear).
  - Pick the first overlapping timeslot and create a Match; mark both requests MATCHED.
  - Advantages: easy to reason about, fast for small datasets, minimal complexity.
  - Limitations: not guaranteed to be maximum-cardinality in all cases.
- Future upgrade: Hopcroft–Karp (maximum bipartite matching)
  - Build a bipartite graph between TUTOR and TUTEE requests with one edge per compatible pair (store a chosen overlap timeslot per edge).
  - Run HK to get maximum number of matches.
  - Useful if backlog grows or fairness requires optimal throughput.
  - Alternative: “greedy + augmenting paths” to improve matching without external libraries.

9) Configuration and environment variables
- Key file: src/main/resources/application.properties
  - Default local (non-container) DB:
    - spring.datasource.url=jdbc:postgresql://localhost:5432/student_portal
    - spring.datasource.username=student_portal
    - spring.datasource.password=student_portal
  - Hibernate: spring.jpa.hibernate.ddl-auto=update (convenient in dev)
  - Thymeleaf cache disabled for development.

- In containers (docker/docker-compose.yml), the app service overrides DB settings via environment variables:
  - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/student_portal
  - SPRING_DATASOURCE_USERNAME=student_portal
  - SPRING_DATASOURCE_PASSWORD=student_portal

- Change ports/user/password safely by editing docker-compose.yml and application.properties (or use env vars).

10) Database seeding (data.sql)
- src/main/resources/data.sql runs at app startup and populates:
  - Subjects (e.g., Mathematics, Physics, Computer Science, …).
  - Timeslots for Mon–Fri, Periods 1–7.
- Seeding is idempotent (uses ON CONFLICT to avoid duplicates).
- If you wipe the DB volume and restart, data.sql runs again.

11) Running locally with containers
Choose one runtime (do not mix in a single session).

A) Colima (recommended for macOS 12)
- Install prerequisites:
  - brew install colima docker
  - If Colima complains about qemu, run: brew install qemu
- Start Colima:
  - colima start --vm-type=qemu --cpu 2 --memory 4 --disk 20
  - docker context use colima
  - docker info (should show Server info)
- Build and run:
  - cd docker
  - docker compose up --build
- Visit:
  - http://localhost:8080

B) OrbStack (simple alternative, supports macOS 12)
- Install OrbStack:
  - brew install --cask orbstack
  - Open the OrbStack app (Docker engine starts automatically).
- Build and run:
  - cd docker
  - docker compose up --build
- Visit:
  - http://localhost:8080

C) Docker Desktop (only if macOS 13+)
- Start Docker Desktop and ensure “Engine running”.
- Build and run:
  - cd docker
  - docker compose up --build
- Visit:
  - http://localhost:8080

D) Multipass (Ubuntu VM) + Docker inside VM (alternative path)
- Install Multipass:
  - brew install --cask multipass
- Create a VM:
  - multipass launch 24.04 --name dockervm --disk 20G --mem 4G --cpus 2
- Install Docker inside VM:
  - multipass shell dockervm
  - Install Docker CE and Compose plugin (see main README for commands).
- Mount project:
  - multipass mount /path/to/student-matching-app dockervm:/home/ubuntu/student-matching-app
- Run:
  - cd /home/ubuntu/student-matching-app/docker
  - docker compose up --build
- Visit:
  - http://<VM_IP>:8080 (get IP via multipass list)

12) Common commands (start/stop/reset/logs)
- Start with build and logs:
  - cd docker
  - docker compose up --build
- Start detached (background):
  - docker compose up -d
- Tail logs:
  - docker compose logs -f app
  - docker compose logs -f db
- Stop containers:
  - Ctrl + C (if foreground) or docker compose down
- Full reset (also deletes DB data volume):
  - docker compose down -v
  - docker compose up --build

13) Troubleshooting guide
- “Cannot connect to the Docker daemon”
  - Start your runtime (Colima or OrbStack).
  - For Colima: colima start --vm-type=qemu; docker context use colima; docker info.
- Port already in use (5432 or 8080)
  - Edit docker/docker-compose.yml:
    - Change DB: "5433:5432"
    - Change App: "8081:8080"
  - Re-run and visit http://localhost:8081
- App can’t connect to DB on first try
  - The DB has a healthcheck; if the app raced ahead, just Ctrl+C and re-run docker compose up.
- Maven “not found” during image build
  - Ensure docker/Dockerfile uses a Maven builder image (e.g., maven:3.9-eclipse-temurin-17).
  - Then run: docker compose build --no-cache; docker compose up.
- Verify DB contents
  - docker exec -it student_portal_db psql -U student_portal -d student_portal
  - At psql prompt:
    - \dt
    - SELECT COUNT(*) FROM subjects;
  - Exit: \q

14) Deployment on a server
- On a Linux server, install Docker + Compose plugin (or use rootless Docker).
- Copy this repository (git clone, or scp).
- Set environment variables or edit docker/docker-compose.yml for production secrets (DB password, etc.).
- Run:
  - cd docker
  - docker compose up --build -d
- Put a reverse proxy (Nginx/Traefik) in front for HTTPS at ports 80/443:
  - Proxy to the app service at port 8080.
- Persist data:
  - Named volume for Postgres is already defined (postgres_data). Back it up periodically.

15) FAQ and next steps
- Q: Why greedy matching rather than Hopcroft–Karp?
  - A: Simpler and perfectly fine at small to mid scale. If you later observe many compatible requests left unmatched, switch to a maximum matching algorithm like Hopcroft–Karp or add augmenting paths to improve greedy results.
- Q: Where do I add new subjects or timeslots?
  - A: For dev/demo, add to data.sql. For production, consider an admin UI or migration scripts.
- Q: How do I create an admin user?
  - A: Register with an email that does NOT start with a digit (e.g., admin@bromsgrove-school.co.uk).
- Next steps you might want:
  - Add a manual “Run matching now” button for admins.
  - Allow tutors to take multiple tutees (capacity) — requires algorithm changes.
  - Add profile UIs to pick subjects and availability.
  - Add email notifications on match creation.
  - Write unit/integration tests (services and matching).

Appendix — Request lifecycle (concise)
1) Controller receives the form (RequestController).
2) Service validates and creates the Request (RequestService).
3) Repository persists it (RequestRepository).
4) Weekly job (MatchingAlgorithm) pairs compatible requests and saves a Match (MatchService).
5) User views Match on /matches; Admin monitors system at /admin.

If you have any questions or want code pointers for a specific area, open an issue or ask for a walkthrough of that module.