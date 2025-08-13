# Student Matching App

School Student Portal for tutor/tutee matching.

Tech stack:
- Java 17, Spring Boot 3.x
- Spring Security, Thymeleaf
- PostgreSQL
- Docker

Quick start:
1. Docker (recommended):
   - cd docker
   - docker compose up --build
   - App: http://localhost:8080

2. Local:
   - Ensure PostgreSQL is running and matches application.properties
   - mvn spring-boot:run

Login/Registration:
- Register with you@bromsgrove-school.co.uk
- Emails starting with a digit become STUDENT, others ADMIN.

Matching:
- A weekly scheduled job runs each Monday 02:30 UTC and performs greedy matching
  on outstanding tutor/tutee requests with overlapping timeslots and year constraints.