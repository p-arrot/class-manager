// Generic API response wrapper
export interface R<T> {
  code: number
  msg: string
  data: T
}

// Paginated response
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// Pagination query params
export interface PageQuery {
  page?: number
  size?: number
  keyword?: string
}

// ========== Auth ==========

export interface LoginRequest {
  account: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  name: string
  role: 'admin' | 'teacher' | 'student'
  classId: number | null
}

// ========== Class ==========

export interface ClassVO {
  id: number
  grade: string
  name: string
  createdAt: string
  updatedAt: string
}

export interface ClassCreateDTO {
  grade: string
  name: string
}

export interface ClassUpdateDTO {
  grade: string
  name: string
}

export interface ClassPageQuery extends PageQuery {
  grade?: string
}

// ========== Teacher ==========

export interface TeacherVO {
  id: number
  username: string
  name: string
  phone: string | null
  email: string | null
  enabled: boolean
  classIds: number[]
  createdAt: string
  updatedAt: string
}

export interface TeacherCreateDTO {
  username: string
  name: string
  password: string
}

export interface TeacherUpdateDTO {
  name?: string
  phone?: string
  email?: string
  enabled?: boolean
}

export interface TeacherClassVO {
  id: number
  classId: number
  grade: string | null
  className: string | null
  createdAt: string
}

export interface BatchBindDTO {
  classIds: number[]
}

// ========== Student ==========

export interface StudentVO {
  id: number
  studentNo: string
  name: string
  classId: number | null
  grade: string | null
  className: string | null
  phone: string | null
  email: string | null
  enabled: boolean
  createdAt: string
}

export interface StudentImportResultVO {
  successCount: number
  failCount: number
  errors: ImportError[]
}

export interface ImportError {
  rowNum: number
  studentNo: string
  name: string
  errorMsg: string
}

export interface StudentPageQuery extends PageQuery {
  classId?: number
}

export interface PasswordResetDTO {
  newPassword?: string
}

export interface StudentCreateDTO {
  studentNo: string
  name: string
  classId: number
  password?: string
}

export interface StudentUpdateDTO {
  name?: string
  classId?: number
  enabled?: boolean
}

export interface StudentBatchDTO {
  ids: number[]
  newPassword?: string
}

// ========== Course ==========

export interface CourseVO {
  id: number
  name: string
  description: string | null
  coverUrl: string | null
  teacherId: number
  teacherName: string | null
  classCount: number
  createdAt: string
  updatedAt: string
}

export interface CourseDetailVO {
  id: number
  name: string
  description: string | null
  coverUrl: string | null
  teacherId: number
  teacherName: string | null
  classCount: number
  semesters: SemesterVO[]
  classIds: number[]
  createdAt: string
  updatedAt: string
}

export interface CourseCreateDTO {
  name: string
  description?: string
  coverUrl?: string
  classIds?: number[]
}

export interface CourseUpdateDTO {
  name?: string
  description?: string
  coverUrl?: string
  classIds?: number[]
}

export interface CoursePageQuery extends PageQuery {
  keyword?: string
  classId?: number
}

// ========== Semester ==========

export interface SemesterVO {
  id: number
  name: string
  startTime: string
  endTime: string
  courseId: number
  lessonCount: number
  createdAt: string
  updatedAt: string
}

export interface SemesterCreateDTO {
  name: string
  startTime: string
  endTime: string
}

export type SemesterUpdateDTO = SemesterCreateDTO

// ========== Lesson ==========

export interface LessonVO {
  id: number
  name: string
  sortOrder: number
  semesterId: number
  createdAt: string
  updatedAt: string
}

export interface LessonCreateDTO {
  name: string
}

export interface LessonUpdateDTO {
  name: string
}

export interface LessonSortDTO {
  targetIndex: number
}

// ========== CourseResource ==========

export interface CourseResourceVO {
  id: number
  name: string
  courseId: number
  parentId: number | null
  type: string
  sortOrder: number
  fileSize: number | null
  contentType: string | null
  children: CourseResourceVO[]
  createdAt: string
}

// ========== File (Phase 3b / F2) ==========

export interface FileUploadDTO {
  fileName: string
  contentType: string
  fileSize: number
  courseId: number
  parentId?: number | null
}

export interface FileUploadVO {
  presignedUrl?: string
  resourceId: number
}
