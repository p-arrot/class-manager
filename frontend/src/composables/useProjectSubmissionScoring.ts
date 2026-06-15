import { computed, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { getDrivePreview, getDriveRaw } from '@/api/drive'
import { listProjectSubmissions, scoreProjectSubmission } from '@/api/projects'
import { getErrorMessage } from '@/utils/error'
import { parseProjectDescription, parseProjectSubmissionContent } from '@/types/project'
import type { ArtifactFile } from '@/types/grading'
import type { ProjectSubmissionVO, ProjectVO } from '@/types/api'
import type { ProjectSubmissionRow } from '@/types/project'

export function useProjectSubmissionScoring() {
  const message = useMessage()
  const showSubmissions = ref(false)
  const submissions = ref<ProjectSubmissionVO[]>([])
  const activeProject = ref<ProjectVO | null>(null)
  const previewUrl = ref('')
  const previewTitle = ref('')
  const previewLoading = ref(false)
  const projectScores = ref<Record<number, Record<string, number>>>({})

  const submissionRows = computed<ProjectSubmissionRow[]>(() => submissions.value.map(submission => ({
    ...submission,
    parsed: parseProjectSubmissionContent(submission.content),
  })))

  const activeProjectRubric = computed(() => parseProjectDescription(activeProject.value).rubric)
  const submissionModalTitle = computed(() => activeProject.value ? `${activeProject.value.name} · 提交情况` : '提交情况')

  async function openSubmissions(project: ProjectVO) {
    activeProject.value = project
    showSubmissions.value = true
    try {
      submissions.value = await listProjectSubmissions(project.id)
    } catch (e) {
      submissions.value = []
      message.error(getErrorMessage(e, '加载提交失败'))
    }
  }

  async function previewFile(file: ArtifactFile) {
    previewLoading.value = true
    previewTitle.value = file.name
    previewUrl.value = ''
    try {
      const data = await getDrivePreview(file.id)
      previewUrl.value = data.url
    } catch (e) {
      message.error(getErrorMessage(e, '预览失败'))
    } finally {
      previewLoading.value = false
    }
  }

  async function downloadFile(file: ArtifactFile) {
    try {
      const blob = await getDriveRaw(file.id)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = file.name
      link.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      message.error(getErrorMessage(e, '下载失败'))
    }
  }

  function closePreview() {
    previewTitle.value = ''
    previewUrl.value = ''
  }

  function getProjectScore(submissionId: number | null, dimension: string) {
    if (!submissionId) return 0
    return projectScores.value[submissionId]?.[dimension] ?? 0
  }

  function setProjectScore(submissionId: number | null, dimension: string, value: number | null) {
    if (!submissionId) return
    if (!projectScores.value[submissionId]) projectScores.value[submissionId] = {}
    projectScores.value[submissionId][dimension] = Math.max(0, Number(value ?? 0))
  }

  async function saveProjectScore(row: ProjectSubmissionRow) {
    const submissionId = row.submissionId ?? row.id
    if (!submissionId) {
      message.warning('该学生尚未提交项目作品，不能评分')
      return
    }
    const rubric = activeProjectRubric.value.filter(item => item.maxScore > 0)
    if (!rubric.length) {
      message.warning('项目未设置评分维度')
      return
    }
    try {
      await scoreProjectSubmission(submissionId, rubric.map(item => ({
        questionId: 'project',
        dimension: item.dimension,
        earnedScore: getProjectScore(submissionId, item.dimension),
        maxScore: item.maxScore,
      })))
      message.success('评分已保存')
    } catch (e) {
      message.error(getErrorMessage(e, '评分失败'))
    }
  }

  return {
    showSubmissions,
    submissionRows,
    activeProjectRubric,
    submissionModalTitle,
    previewUrl,
    previewTitle,
    previewLoading,
    openSubmissions,
    previewFile,
    downloadFile,
    closePreview,
    getProjectScore,
    setProjectScore,
    saveProjectScore,
  }
}
