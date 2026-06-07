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

// ========== Task (Phase 4 / F3) ==========

export interface TaskVO {
  id: number
  title: string
  type: 'worksheet' | 'artifact'
  lessonId: number
  description: string | null
  deadline: string | null
  submissionCount: number
  createdAt: string
  updatedAt: string
}

export interface TaskDetailVO extends TaskVO {
  formSchema: string | null
}

export interface TaskCreateDTO {
  title: string
  type: 'worksheet' | 'artifact'
  formSchema?: string
  description?: string
  deadline?: string
}

export interface TaskUpdateDTO {
  title?: string
  formSchema?: string
  description?: string
  deadline?: string
}

export interface SubmissionVO {
  id: number
  taskId: number
  studentId: number
  studentName: string | null
  studentNo: string | null
  status: string
  content: string
  submittedAt: string | null
  createdAt: string
}

export interface TaskAnalyticsVO {
  taskId: number
  title: string
  type: 'worksheet' | 'artifact'
  totalStudents: number
  submittedCount: number
  gradedCount: number
  specialCount: number
  notSubmittedCount: number
  submissionRate: number
  accuracyRate: number
  questions: QuestionAnalyticsVO[]
  submissions: StudentTaskAnswerVO[]
}

export interface QuestionAnalyticsVO {
  questionId: string
  index: number
  type: string
  stem: string
  autoGradable: boolean
  answerCount: number
  correctCount: number
  accuracyRate: number
  optionDistribution: Record<string, number>
  answers: StudentAnswerVO[]
}

export interface StudentAnswerVO {
  submissionId: number
  studentId: number
  studentName: string | null
  studentNo: string | null
  status: string
  answer: unknown
  correct: boolean | null
  submittedAt: string | null
}

export interface StudentTaskAnswerVO {
  submissionId: number
  studentId: number
  studentName: string | null
  studentNo: string | null
  status: string
  content: string | null
  submittedAt: string | null
}

export interface SubmissionDTO {
  content: string
}

export interface StudentDashboardVO {
  courses: CourseVO[]
  totalCourses: number
  dueTasks: Array<{
    task: TaskVO
    courseName: string
    lessonName: string
    courseId: number | null
  }>
  recentGrades: Array<{
    submission: SubmissionVO
    taskTitle: string
    courseName: string
  }>
}

export interface TeacherDashboardVO {
  pendingGrading: number
  upcomingDeadlines: number
  recentCount: number
  pendingSubmissions: Array<{
    submission: SubmissionVO
    taskTitle: string
    semesterName: string
  }>
  recentSubmissions: Array<{
    submission: SubmissionVO
    taskTitle: string
    semesterName: string
  }>
  upcomingTasks: Array<{
    task: TaskVO
    semesterName: string
    lessonName: string
  }>
}

export interface SubmissionEvaluationSummary {
  autoScore: number | null
  totalAutoScore: number
  correctCount: number
  autoGradableCount: number
}

export interface EvaluationDimensionDTO {
  dimension: string
  grade: string
}

export interface SubmissionEvaluationDTO {
  dimensions: EvaluationDimensionDTO[]
  questionScores?: QuestionDimensionScoreDTO[]
  isSpecial?: boolean
}

export interface QuestionDimensionScoreDTO {
  questionId: string
  dimension: string
  earnedScore: number
  maxScore: number
  autoGraded?: boolean
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

// ========== Evaluation / Stats ==========

export interface RadarPointVO {
  dimension: string
  label: string
  avgScore: number
}

export interface RadarVO {
  current: RadarPointVO[]
  previous?: RadarPointVO[]
  hasPrevious: boolean
}

export interface EvaluationVO {
  dimension: string
  grade: string
  score: number
  label: string
}

export interface SemesterStatsPreviewRow {
  studentId: number
  studentNo: string
  studentName: string
  processScore: number | null
  examScore: number | null
  projectScore: number | null
  totalScore: number | null
  totalGrade: string | null
}

export interface AssessmentSchemeVO {
  id?: number | null
  semesterId: number
  processPercent: number
  examPercent: number
  projectPercent: number
}

export interface AssessmentSchemeDTO {
  processPercent: number
  examPercent: number
  projectPercent: number
}

// ========== Exams / Projects ==========

export interface ExamVO {
  id: number
  name: string
  startTime: string
  endTime: string
  weight: number
  semesterId: number
  paperId?: number | null
  paperContent?: string | null
}

export interface ExamPaperVO {
  id: number
  title: string
  content?: string | null
  totalScore?: number
}

export interface ExamSubmitDTO {
  answers: string
}

export interface ProjectVO {
  id: number
  name: string
  description: string | null
  maxTeamSize: number
  deadline: string | null
  weight?: number
  semesterId: number
}

export interface ProjectSubmitDTO {
  content: string
}

export interface ProjectSubmissionVO {
  id: number
  projectId: number
  teamId: number | null
  studentId: number
  studentName: string | null
  studentNo: string | null
  content: string | null
  submittedAt: string | null
  createdAt: string
}

// ========== Drive ==========

export interface DriveItemVO {
  id: number
  name: string
  type: 'FOLDER' | 'FILE'
  fileSize: number | null
  contentType?: string | null
  parentId?: number | null
  objectName?: string | null
  createdAt: string
}

export interface PreviewUrlVO {
  url: string
}

export interface DriveTreeQuery {
  parentId?: number | null
  userId?: number
}

export interface DriveFolderCreateDTO {
  name: string
  parentId?: number | null
}
