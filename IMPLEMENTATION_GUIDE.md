# Student Portal - Advanced Tutoring Match System

## Overview

This is a comprehensive student tutoring portal for Bromsgrove School, designed to facilitate peer tutoring through an advanced matching algorithm. The system supports the full tutoring lifecycle from request creation to weekly recurring sessions.

## Key Features

### 🎓 Student Portal Functionality
- **User Profiles**: Comprehensive profiles with year group, exam board, subjects, and availability management
- **Request Management**: Create tutor/tutee requests with specific timeslots and subjects
- **Interactive Availability**: Monday-Friday, Period 1-7 grid for setting availability
- **Subject Selection**: Multi-select interface for choosing subjects to tutor or receive help in
- **Dashboard**: Real-time overview of requests, matches, and profile completeness

### 🤝 Advanced Matching System
- **Hopcroft-Karp Algorithm**: Maximum bipartite matching algorithm for optimal tutor-tutee pairing
- **Capacity Constraints**: Respects tutor `maxSessionsPerWeek` limits
- **Conflict Prevention**: Prevents double-booking of tutors at the same timeslot
- **Subject Compatibility**: Matches based on subject requirements
- **Year Group Validation**: Ensures tutors are in same or higher year group than tutees

### 🔄 Recurrence Management  
- **Weekly Recurrence**: Tutees can request weekly recurring sessions
- **Tutor Acceptance**: Tutors can accept/decline recurring requests
- **Auto-Generation**: System automatically creates next week's requests for active recurring pairs
- **Flexible Cancellation**: Either party can cancel recurrence at any time

### ⚙️ Administrative Tools
- **Matching Dashboard**: Real-time statistics and manual matching controls
- **Algorithm Management**: Run matching for specific weeks, generate recurring requests
- **Subject Administration**: Add/remove subjects with safety checks
- **System Monitoring**: Detailed insights into matching efficiency and supply/demand

## Technical Architecture

### Backend (Java 17 + Spring Boot 3.x)
```
src/main/java/com/example/student_portal/
├── entity/                 # JPA Entities
│   ├── User.java          # Enhanced user with availability & capacity
│   ├── Request.java       # Requests with recurrence & target week support  
│   ├── AvailabilitySlot.java # Day/Period availability slots
│   ├── Subject.java       # Academic subjects
│   ├── Timeslot.java      # School timetable periods
│   └── Match.java         # Confirmed tutor-tutee matches
├── service/               # Business Logic Layer
│   ├── MatchingService.java    # Hopcroft-Karp matching implementation
│   ├── RecurrenceService.java  # Weekly recurrence management
│   ├── ValidationService.java  # Business rule validation
│   ├── AvailabilityService.java # Availability slot management
│   └── [Other services...]
├── controller/            # REST Controllers
│   ├── DashboardController.java    # Student homepage
│   ├── AvailabilityController.java # Availability management
│   ├── RequestController.java      # Request lifecycle
│   ├── MatchingAdminController.java # Admin matching tools
│   └── [Other controllers...]
├── util/                  # Algorithm Implementation
│   ├── HopcroftKarp.java      # Maximum bipartite matching
│   └── MatchingAlgorithm.java # Weekly matching orchestrator
├── validation/            # Custom Validation
│   ├── ValidExamBoardForYearGroup.java  # Year group validation
│   └── ExamBoardYearGroupValidator.java
└── model/                 # Enumerations & Models
    ├── YearGroup.java     # Year 9-13 
    ├── ExamBoard.java     # GCSE, IB, A-Levels
    ├── Period.java        # P1-P7 school periods
    ├── Weekday.java       # Monday-Friday
    └── [Other enums...]
```

### Frontend (Thymeleaf + Modern CSS)
```
src/main/resources/templates/
├── dashboard.html              # Student homepage with stats
├── availability.html           # Interactive availability grid
├── subjects/
│   └── manage.html            # Subject selection interface  
├── admin/
│   └── matching/
│       └── dashboard.html     # Admin matching controls
└── fragments/
    └── header.html           # Navigation with dropdowns
```

### Database Schema (PostgreSQL)
- **users**: Enhanced with `max_sessions_per_week`, `teaching_mode`
- **availability_slots**: (user_id, day_of_week, period) with unique constraints
- **requests**: Enhanced with `target_week`, `is_recurring`, `matched_partner_id`
- **subjects**: Academic subjects with usage tracking
- **matches**: Confirmed tutor-tutee pairs with timeslots

## Algorithm Implementation

### Hopcroft-Karp Maximum Bipartite Matching

The matching algorithm creates a bipartite graph where:
- **Left nodes**: Tutee request timeslots (request + specific timeslot)
- **Right nodes**: Tutor availability nodes (tutor + timeslot + session capacity)

**Constraints Enforced:**
1. **Subject Compatibility**: Tutor and tutee must share the subject
2. **Year Group Eligibility**: Tutor year ≥ Tutee year  
3. **Timeslot Availability**: Both parties available at the same time
4. **Capacity Limits**: Respects tutor `maxSessionsPerWeek`
5. **Uniqueness**: No double-booking of tutor timeslots

**Algorithm Complexity:** O(E × √V) where E = edges, V = vertices

### Weekly Lifecycle

1. **Sunday Evening**: 
   - Recurrence service generates new requests for active recurring pairs
   - Matching algorithm runs to create matches for upcoming week

2. **Throughout Week**:
   - Students can create new requests for future weeks
   - Tutees can request weekly recurrence on matched sessions
   - Tutors can accept/decline recurrence requests

3. **Request States**:
   - `OUTSTANDING` → `MATCHED` (via algorithm) → `COMPLETED` (manual)
   - Recurring requests automatically regenerated each week

## Business Rules & Validation

### Profile Requirements
- Full name, year group, and exam board must be set
- At least one subject must be selected
- At least one availability slot must be set
- Max sessions per week must be 1-10

### Exam Board Validation
- **Years 9-11**: Must have `NONE` (GCSE years)
- **Years 12-13**: Must select `IB` or `A_LEVELS`

### Request Validation  
- Users must have selected the subject they're requesting
- Tutors must have availability set before offering tutoring
- No duplicate outstanding requests per user/subject/type
- Tutor requests must include available timeslots

### Matching Validation
- Subject compatibility required
- Year group constraints enforced  
- Timeslot overlap validation
- Capacity limits respected

## User Experience Features

### Dashboard Insights
- **Profile Completeness**: Scoring system encouraging complete profiles
- **Quick Stats**: Visual cards showing requests, matches, and activity
- **Status Tracking**: Clear badges for request and match states

### Availability Management
- **Interactive Grid**: Monday-Friday × Period 1-7 checkbox interface
- **Bulk Operations**: Select multiple slots simultaneously  
- **Visual Feedback**: Current availability summary with badges
- **Mobile Responsive**: Optimized for phone/tablet use

### Request Management
- **Target Week Selection**: Create requests for specific weeks
- **Recurrence Workflow**: Request → Accept → Auto-generation cycle
- **Status Management**: Cancel, complete, and track request lifecycle
- **Validation Feedback**: Clear error messages with guidance

### Administrative Tools
- **Real-time Statistics**: Live matching efficiency and demand analysis
- **Manual Controls**: Run matching for specific weeks or trigger recurrence
- **System Insights**: Supply/demand analysis and optimization suggestions
- **Safety Features**: Confirmation dialogs and rollback capabilities

## Installation & Setup

### Prerequisites
- Java 17+
- PostgreSQL 12+
- Maven 3.6+

### Quick Start
```bash
# Clone repository
git clone [repository-url]
cd student-portal

# Setup PostgreSQL database
createdb student_portal
# Update src/main/resources/application.properties with your DB credentials

# Build and run
./mvnw spring-boot:run
```

### Initial Setup
1. **Database**: Hibernate will create tables on first run
2. **Admin User**: Register with non-numeric email for admin access
3. **Test Data**: Use `/admin/matching` to populate subjects and test matching

## API Endpoints

### Student Endpoints
- `GET /dashboard` - Student homepage
- `GET/POST /profile` - Profile management
- `GET/POST /subjects` - Subject selection  
- `GET/POST /availability` - Availability management
- `GET/POST /requests` - Request management
- `POST /requests/{id}/request-recurrence` - Request weekly recurrence
- `POST /requests/{id}/accept-recurrence` - Accept weekly recurrence

### Admin Endpoints (ADMIN role required)
- `GET /admin/matching` - Matching dashboard
- `POST /admin/matching/run` - Manual matching trigger
- `POST /admin/matching/run-for-week` - Week-specific matching
- `POST /admin/matching/generate-recurring` - Generate recurring requests
- `GET /subjects/admin` - Subject administration

## Configuration

### Application Properties
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/student_portal
spring.datasource.username=student_portal
spring.datasource.password=student_portal

# Matching Schedule (Monday 2:30 AM)
# Configured via @Scheduled annotation in MatchingAlgorithm.java

# Thymeleaf Development
spring.thymeleaf.cache=false
```

### Customization
- **Matching Schedule**: Modify cron expression in `MatchingAlgorithm.java`
- **Capacity Limits**: Adjust validation in `ValidationService.java`  
- **UI Styling**: Update `src/main/resources/static/css/styles.css`
- **Business Rules**: Modify validation logic in service classes

## Future Enhancements

### Planned Features
- **Email Notifications**: Automated emails for matches and recurrence
- **Mobile App**: Native iOS/Android applications
- **Analytics Dashboard**: Advanced reporting and insights
- **Chat Integration**: Built-in messaging between matched pairs
- **Feedback System**: Post-session ratings and feedback

### Technical Improvements  
- **Incremental Matching**: Optimize algorithm for large datasets
- **Caching Layer**: Redis integration for performance
- **API Documentation**: OpenAPI/Swagger integration
- **Testing Suite**: Comprehensive integration and unit tests
- **CI/CD Pipeline**: Automated testing and deployment

## Support & Maintenance

### Monitoring
- Application logs for debugging and performance monitoring
- Database query optimization for large user bases
- Algorithm performance metrics and optimization

### Troubleshooting
- **Matching Issues**: Check admin dashboard for supply/demand imbalances
- **Profile Issues**: Validate exam board/year group compatibility  
- **Performance**: Monitor database queries and consider indexing

---

**Project Status**: Production-ready core functionality with advanced matching algorithm
**License**: MIT License
**Maintainer**: Bromsgrove School IT Department