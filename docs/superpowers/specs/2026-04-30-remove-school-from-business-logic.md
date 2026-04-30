# Remove School from Business Logic

**Date:** 2026-04-30  
**Status:** Approved

## Goal

Remove the concept of "school" from all business logic while keeping the `schools` table and `school_id` columns in the database for potential future use. The system operates within a single school context, so school-level disambiguation is unnecessary.

## Scope

### Backend Code Changes

1. **`LoginDTO`** — Remove `schoolId` field. Login no longer requires or accepts a school identifier.
2. **`LoginVO`** — Remove `schoolId` field from login response.
3. **`LoginUser`** — Remove `schoolId` from the security principal. Remove constructor parameter, getter.
4. **`JwtUtils`** — Remove `schoolId` claim from token generation and parsing.
5. **`AuthServiceImpl`** — Flatten login logic:
   - Admin: match by `username` where `role = 'admin'`
   - Teacher: match by `username` where `role = 'teacher'`
   - Student: match by `student_no` where `role = 'student'`
   - All three queries become independent (no branching on `schoolId` presence).
   - `username` and `student_no` must be globally unique.

### Database Changes (Flyway V2)

- Drop `idx_users_school_student_no` unique index
- Create `idx_users_student_no` unique index on `student_no` alone (where `student_no IS NOT NULL`)
- Create `idx_users_username_role` unique index on `username` alone (where `username IS NOT NULL`) — since teacher usernames must now be globally unique
- `school_id` columns remain as-is (nullable, not dropped, not modified)
- Foreign keys to `schools` remain
- `schools` table remains

### Documentation Changes

- `docs/project-prompt.md` — Update login flow description
- `docs/API.md` — Update login endpoint documentation

## What Does NOT Change

- `schools` table stays
- `school_classes.school_id` stays (FK to schools)
- `users.school_id` stays (nullable, unused in logic)
- All existing foreign key constraints stay
- `AdminInitializer` already creates admin without `schoolId` — no change needed

## Login Flow (New)

```
POST /api/auth/login
Body: { account, password }
  - No schoolId needed

1. Try admin:   SELECT * FROM users WHERE username = account AND role = 'admin'
2. Try teacher: SELECT * FROM users WHERE username = account AND role = 'teacher'
3. Try student: SELECT * FROM users WHERE student_no = account AND role = 'student'
4. First match wins, check password, return token
```

## Files to Modify

| File | Change |
|------|--------|
| `backend/src/main/java/com/example/edu/modules/auth/dto/LoginDTO.java` | Remove `schoolId` field |
| `backend/src/main/java/com/example/edu/modules/auth/vo/LoginVO.java` | Remove `schoolId` field |
| `backend/src/main/java/com/example/edu/common/security/LoginUser.java` | Remove `schoolId` field |
| `backend/src/main/java/com/example/edu/common/security/JwtUtils.java` | Remove `schoolId` claim |
| `backend/src/main/java/com/example/edu/modules/auth/service/impl/AuthServiceImpl.java` | Flatten login logic |
| `backend/src/main/resources/db/migration/V2__remove_school_unique.sql` | New: index changes |
| `docs/project-prompt.md` | Update login section |
| `docs/API.md` | Update login endpoint docs |
