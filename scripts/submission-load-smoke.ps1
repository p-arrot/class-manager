param(
    [string]$BaseUrl = 'http://127.0.0.1:18080',
    [int]$SubmissionCount = 30,
    [string]$AdminPassword = 'admin123',
    [string]$TeacherPassword = '123456',
    [string]$StudentPassword = '123456'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

function Invoke-Api {
    param(
        [ValidateSet('GET', 'POST', 'PUT', 'DELETE')][string]$Method,
        [string]$Path,
        [string]$Token,
        $Body
    )
    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        ContentType = 'application/json'
        TimeoutSec = 30
    }
    if ($Token) { $params.Headers = @{ Authorization = "Bearer $Token" } }
    if ($null -ne $Body) { $params.Body = $Body | ConvertTo-Json -Depth 12 -Compress }
    try {
        $response = Invoke-RestMethod @params
    } catch {
        $details = $_.ErrorDetails.Message
        if (-not $details -and $_.Exception.Response) {
            $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            try { $details = $reader.ReadToEnd() } finally { $reader.Dispose() }
        }
        throw "$Method $Path failed: $($_.Exception.Message) $details"
    }
    if ($response.code -ne 0) { throw "$Method $Path failed: $($response.code) $($response.msg)" }
    return $response.data
}

function Login([string]$Account, [string]$Password) {
    return (Invoke-Api POST '/api/auth/login' '' @{ account = $Account; password = $Password }).token
}

function Invoke-MultipartUpload {
    param(
        [string]$Path,
        [string]$Token,
        [string[]]$Form
    )
    $arguments = @('-sS', '-X', 'POST', "$BaseUrl$Path", '-H', "Authorization: Bearer $Token")
    foreach ($field in $Form) { $arguments += @('-F', $field) }
    $raw = & curl.exe @arguments
    if ($LASTEXITCODE -ne 0) { throw "Multipart upload failed: $Path" }
    $response = $raw | ConvertFrom-Json
    if ($response.code -ne 0) { throw "Multipart upload failed: $Path, $($response.code) $($response.msg)" }
    return $response.data
}

$runId = Get-Date -Format 'MMddHHmmss'
$adminToken = Login 'admin' $AdminPassword
$classes = Invoke-Api GET '/api/classes/list-all' $adminToken $null
$studentsPage = Invoke-Api GET '/api/students?page=1&size=200' $adminToken $null
$students = @($studentsPage.records | Where-Object { $_.enabled -and $_.classId } | Select-Object -First $SubmissionCount)
if ($students.Count -lt $SubmissionCount) { throw "Only $($students.Count) enabled students are available" }
$classIds = @($students.classId | Sort-Object -Unique)

$teacher = Invoke-Api POST '/api/teachers' $adminToken @{
    username = "load_teacher_$runId"
    name = "Load Test Teacher $runId"
    password = $TeacherPassword
}
Invoke-Api POST "/api/teachers/$($teacher.id)/classes" $adminToken @{ classIds = $classIds } | Out-Null
$teacherToken = Login $teacher.username $TeacherPassword

$course = Invoke-Api POST '/api/courses' $teacherToken @{
    name = "Load Validation Course $runId"
    description = 'Docker container scenario validation'
    classIds = $classIds
}
$semester = Invoke-Api POST "/api/courses/$($course.id)/semesters" $teacherToken @{
    name = "Validation Semester $runId"
    startTime = (Get-Date).AddDays(-1).ToString('yyyy-MM-ddTHH:mm:ss')
    endTime = (Get-Date).AddDays(30).ToString('yyyy-MM-ddTHH:mm:ss')
}
$lesson = Invoke-Api POST "/api/semesters/$($semester.id)/lessons" $teacherToken @{ name = 'Lesson 1: Information Expression' }
$task = Invoke-Api POST "/api/lessons/$($lesson.id)/tasks" $teacherToken @{
    title = "Classroom Task $runId"
    type = 'artifact'
    description = 'Submit an individual reflection based on the classroom example.'
    deadline = (Get-Date).AddHours(2).ToString('yyyy-MM-ddTHH:mm:ss')
}

$studentSessions = foreach ($student in $students) {
    [pscustomobject]@{
        Student = $student
        Token = Login $student.studentNo $StudentPassword
    }
}

$http = [System.Net.Http.HttpClient]::new()
$pending = @()
$requests = @()
$watch = [System.Diagnostics.Stopwatch]::StartNew()
foreach ($session in $studentSessions) {
    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::Post,
        "$BaseUrl/api/tasks/$($task.id)/submit")
    $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $session.Token)
    $content = @{ content = (@{
        note = "$($session.Student.name) classroom reflection"
        files = @()
    } | ConvertTo-Json -Compress) } | ConvertTo-Json -Compress
    $request.Content = [System.Net.Http.StringContent]::new($content, [Text.Encoding]::UTF8, 'application/json')
    $requests += $request
    $pending += $http.SendAsync($request)
}

$responses = foreach ($operation in $pending) { $operation.GetAwaiter().GetResult() }
$watch.Stop()
$submissionRows = @()
foreach ($response in $responses) {
    $payload = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
    if (-not $response.IsSuccessStatusCode -or $payload.code -ne 0) {
        throw "Concurrent submission failed: HTTP $([int]$response.StatusCode), code=$($payload.code), msg=$($payload.msg)"
    }
    $submissionRows += $payload.data
}
$requests | ForEach-Object Dispose
$responses | ForEach-Object Dispose
$http.Dispose()

$analytics = Invoke-Api GET "/api/tasks/$($task.id)/analytics" $teacherToken $null
if ($analytics.submittedCount -lt $SubmissionCount) {
    throw "Expected at least $SubmissionCount submitted rows, got $($analytics.submittedCount)"
}

$firstSession = $studentSessions[0]
$firstSubmission = $submissionRows | Where-Object { $_.studentId -eq $firstSession.Student.id } | Select-Object -First 1
Invoke-Api PUT "/api/submissions/$($firstSubmission.id)/return" $teacherToken @{ reason = 'Add a classroom example and personal conclusion.' } | Out-Null
$resubmitted = Invoke-Api POST "/api/tasks/$($task.id)/submit" $firstSession.Token @{
    content = (@{ note = 'Added an example, reasoning, and a personal conclusion.'; files = @() } | ConvertTo-Json -Compress)
}
if ($resubmitted.status -ne 'submitted' -or $resubmitted.revisionCount -ne 1) {
    throw "Unexpected resubmission state: $($resubmitted.status), revision=$($resubmitted.revisionCount)"
}

Invoke-Api POST "/api/submissions/$($firstSubmission.id)/evaluate" $teacherToken @{
    dimensions = @(
        @{ dimension = 'AWARENESS'; grade = 'A' },
        @{ dimension = 'COMPUTING'; grade = 'B' },
        @{ dimension = 'DIGITAL_LEARNING'; grade = 'A' },
        @{ dimension = 'RESPONSIBILITY'; grade = 'A' }
    )
    teacherComment = 'The example is complete and the conclusion is clear.'
    isSpecial = $false
}

$lockedStatus = 0
try {
    Invoke-WebRequest -Method POST -Uri "$BaseUrl/api/tasks/$($task.id)/submit" `
        -Headers @{ Authorization = "Bearer $($firstSession.Token)" } `
        -ContentType 'application/json' `
        -Body (@{ content = '{"note":"attempt overwrite"}' } | ConvertTo-Json -Compress) `
        -TimeoutSec 30 | Out-Null
} catch {
    $lockedStatus = [int]$_.Exception.Response.StatusCode
}
if ($lockedStatus -ne 409) { throw "Expected HTTP 409 after grading, got $lockedStatus" }

$result = Invoke-Api GET "/api/tasks/$($task.id)/my-result" $firstSession.Token $null
if ($result.status -ne 'graded' -or $result.submission.teacherComment -ne 'The example is complete and the conclusion is clear.') {
    throw "Student grading detail did not contain the expected result and comment: $($result | ConvertTo-Json -Depth 8 -Compress)"
}

$resourceFolder = Invoke-Api POST "/api/courses/$($course.id)/resources" $teacherToken @{
    name = "Semester Materials $runId"
}
$uploadFile = (Resolve-Path (Join-Path $PSScriptRoot '..\README.md')).Path
$courseFile = Invoke-MultipartUpload '/api/files/upload' $teacherToken @(
    "file=@$uploadFile;type=text/markdown",
    "courseId=$($course.id)",
    "parentId=$($resourceFolder.id)"
)
$studentResourceTree = @(Invoke-Api GET "/api/courses/$($course.id)/resources/tree" $firstSession.Token $null)
$visibleResourceFolder = $studentResourceTree | Where-Object { $_.id -eq $resourceFolder.id } | Select-Object -First 1
if (-not $visibleResourceFolder -or -not ($visibleResourceFolder.children | Where-Object { $_.id -eq $courseFile.resourceId })) {
    throw 'Student could not see the uploaded course resource'
}
$courseDownload = Invoke-Api GET "/api/files/$($courseFile.resourceId)/download" $firstSession.Token $null
if (-not $courseDownload.url) { throw 'Course resource did not provide a download URL' }

$driveFolder = Invoke-Api POST '/api/drive/folders' $firstSession.Token @{ name = "Course Work $runId" }
$driveFile = Invoke-MultipartUpload '/api/drive/upload' $firstSession.Token @(
    "file=@$uploadFile;type=text/markdown",
    "parentId=$($driveFolder.id)"
)
$driveChildren = @(Invoke-Api GET "/api/drive/tree?parentId=$($driveFolder.id)" $firstSession.Token $null)
if (-not ($driveChildren | Where-Object { $_.id -eq $driveFile.id })) {
    throw 'Uploaded student drive file was not listed'
}
$driveDownload = Invoke-Api GET "/api/drive/$($driveFile.id)/download" $firstSession.Token $null
if (-not $driveDownload.url) { throw 'Student drive file did not provide a download URL' }
$otherStudentStatus = 0
try {
    Invoke-WebRequest -Method GET -Uri "$BaseUrl/api/drive/tree?userId=$($studentSessions[1].Student.id)" `
        -Headers @{ Authorization = "Bearer $($firstSession.Token)" } -TimeoutSec 30 | Out-Null
} catch {
    $otherStudentStatus = [int]$_.Exception.Response.StatusCode
}
if ($otherStudentStatus -ne 403) { throw "Expected HTTP 403 for another student's drive, got $otherStudentStatus" }

$paperSchema = @{
    version = 3
    questions = @(
        @{
            id = 'q1'
            type = 'text'
            stem = 'Explain how to verify information from two independent sources.'
            score = 20
            autoGrade = $false
            dimensionScores = @(@{ dimension = 'AWARENESS'; maxScore = 20 })
        }
    )
} | ConvertTo-Json -Depth 8 -Compress
$paper = Invoke-Api POST '/api/exam-papers' $teacherToken @{
    title = "Information Verification Paper $runId"
    content = $paperSchema
    totalScore = 20
}
$exam = Invoke-Api POST "/api/semesters/$($semester.id)/exams" $teacherToken @{
    name = "Midterm Scenario $runId"
    paperId = $paper.id
    startTime = (Get-Date).AddMinutes(-5).ToString('yyyy-MM-ddTHH:mm:ss')
    endTime = (Get-Date).AddHours(2).ToString('yyyy-MM-ddTHH:mm:ss')
    weight = 0.3
}
$startedExam = Invoke-Api POST "/api/exams/$($exam.id)/start" $firstSession.Token @{}
if ($startedExam.status -ne 'in_progress' -or -not $startedExam.startedAt) { throw 'Exam did not enter in_progress state' }
$draftAnswer = @{ q1 = 'Compare the author, evidence, date, and an independent source.' } | ConvertTo-Json -Compress
$savedDraft = Invoke-Api PUT "/api/exams/$($exam.id)/draft" $firstSession.Token @{ answers = $draftAnswer }
if ($savedDraft.status -ne 'in_progress' -or $savedDraft.answers -ne $draftAnswer) { throw 'Exam draft was not saved' }
$examSubmission = Invoke-Api POST "/api/exams/$($exam.id)/submit" $firstSession.Token @{ answers = $draftAnswer }
$examInbox = @(Invoke-Api GET "/api/exams/$($exam.id)/submissions" $teacherToken $null)
$examRow = $examInbox | Where-Object { $_.studentId -eq $firstSession.Student.id } | Select-Object -First 1
if ($examInbox.Count -lt $students.Count -or $examRow.status -ne 'submitted') { throw 'Exam roster did not show the complete expected student list' }
Invoke-Api PUT "/api/exam-submissions/$($examSubmission.id)/return" $teacherToken @{ reason = 'Add a concrete source comparison.' } | Out-Null
$returnedExam = Invoke-Api GET "/api/exams/$($exam.id)/my-submission" $firstSession.Token $null
if ($returnedExam.status -ne 'returned' -or -not $returnedExam.returnReason) { throw 'Student could not see the exam return reason' }
$revisedAnswer = @{ q1 = 'Compare author, evidence, date, and verify the claim against an official source.' } | ConvertTo-Json -Compress
$revisedExam = Invoke-Api POST "/api/exams/$($exam.id)/submit" $firstSession.Token @{ answers = $revisedAnswer }
if ($revisedExam.revisionCount -ne 1) { throw 'Exam revision count was not incremented' }
Invoke-Api PUT "/api/exam-submissions/$($examSubmission.id)/grade" $teacherToken @{
    score = 18
    absent = $false
    dimensionScores = @(@{ questionId = 'q1'; dimension = 'AWARENESS'; earnedScore = 18; maxScore = 20; autoGraded = $false })
} | Out-Null
$gradedExam = Invoke-Api GET "/api/exams/$($exam.id)/my-submission" $firstSession.Token $null
if ($gradedExam.status -ne 'graded' -or $gradedExam.score -ne 18) { throw 'Student could not see the graded exam result' }

$projectDescription = @{
    text = 'Create an individual digital story and explain any collaboration in the note.'
    artifact = @{ submitMode = 'file'; allowedExtensions = @('md', 'pdf') }
    rubric = @(
        @{ dimension = 'AWARENESS'; maxScore = 10 },
        @{ dimension = 'COMPUTING'; maxScore = 20 },
        @{ dimension = 'DIGITAL_LEARNING'; maxScore = 10 },
        @{ dimension = 'RESPONSIBILITY'; maxScore = 10 }
    )
} | ConvertTo-Json -Depth 8 -Compress
$project = Invoke-Api POST "/api/semesters/$($semester.id)/projects" $teacherToken @{
    name = "Digital Story Project $runId"
    description = $projectDescription
    deadline = (Get-Date).AddDays(7).ToString('yyyy-MM-ddTHH:mm:ss')
    weight = 0.3
}
$projectContent = @{
    note = 'Individual submission. Collaborators discussed ideas only: student 2024002.'
    submitMode = 'file'
    files = @(@{ id = $driveFile.id; name = $driveFile.name; fileSize = $driveFile.fileSize; type = 'FILE' })
} | ConvertTo-Json -Depth 8 -Compress
$projectSubmission = Invoke-Api POST "/api/projects/$($project.id)/submit" $firstSession.Token @{ content = $projectContent }
$projectInbox = @(Invoke-Api GET "/api/projects/$($project.id)/submissions" $teacherToken $null)
$projectRow = $projectInbox | Where-Object { $_.studentId -eq $firstSession.Student.id } | Select-Object -First 1
if ($projectInbox.Count -lt $students.Count -or $projectRow.status -ne 'submitted' -or $projectRow.content -notmatch 'Collaborators') {
    throw 'Project roster did not include the full class or the student note'
}
Invoke-Api PUT "/api/project-submissions/$($projectSubmission.id)/return" $teacherToken @{ reason = 'Add a short reflection on source selection.' } | Out-Null
$returnedProject = Invoke-Api GET "/api/projects/$($project.id)/my-submission" $firstSession.Token $null
if ($returnedProject.status -ne 'returned' -or -not $returnedProject.returnReason) { throw 'Student could not see the project return reason' }
$revisedProject = Invoke-Api POST "/api/projects/$($project.id)/submit" $firstSession.Token @{
    content = (@{ note = 'Added source selection reflection. Collaborator: student 2024002.'; submitMode = 'file'; files = @() } | ConvertTo-Json -Compress)
}
if ($revisedProject.revisionCount -ne 1) { throw 'Project revision count was not incremented' }
Invoke-Api POST "/api/project-submissions/$($projectSubmission.id)/score" $teacherToken @(
    @{ questionId = 'project'; dimension = 'AWARENESS'; earnedScore = 8; maxScore = 10 },
    @{ questionId = 'project'; dimension = 'COMPUTING'; earnedScore = 18; maxScore = 20 },
    @{ questionId = 'project'; dimension = 'DIGITAL_LEARNING'; earnedScore = 9; maxScore = 10 },
    @{ questionId = 'project'; dimension = 'RESPONSIBILITY'; earnedScore = 10; maxScore = 10 }
) | Out-Null
$gradedProject = Invoke-Api GET "/api/projects/$($project.id)/my-submission" $firstSession.Token $null
if ($gradedProject.status -ne 'graded' -or $gradedProject.score -ne 45 -or $gradedProject.dimensionScores.Count -ne 4) {
    throw "Student project grading detail is incomplete: $($gradedProject | ConvertTo-Json -Depth 8 -Compress)"
}

[pscustomobject]@{
    RunId = $runId
    CourseId = $course.id
    SemesterId = $semester.id
    TaskId = $task.id
    Students = $SubmissionCount
    ConcurrentSubmitMilliseconds = $watch.ElapsedMilliseconds
    SubmittedCount = $analytics.submittedCount
    ReturnedAndResubmitted = $true
    GradedWriteLockedHttpStatus = $lockedStatus
    StudentCanReadGradingDetail = $true
    CourseResourceVisible = $true
    StudentDriveUploadAndDownload = $true
    OtherStudentDriveHttpStatus = $otherStudentStatus
    ExamDraftReturnGradeFlow = $true
    ExamRosterStudents = $examInbox.Count
    ProjectReturnGradeDetailFlow = $true
    ProjectRosterStudents = $projectInbox.Count
    ProjectScore = $gradedProject.score
}
