import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, nextTick, ref } from 'vue'
import ProjectView from '@/views/student/ProjectView.vue'
import { getMyProjectSubmission, listProjects, submitProject } from '@/api/projects'
import type { ProjectSubmissionVO, ProjectVO } from '@/types/api'

const courses = ref([{ id: 1, name: 'Information Technology' }])
const semesters = ref([{
  id: 2,
  courseId: 1,
  name: '2026 Spring',
  startTime: '2026-02-01T00:00:00',
  endTime: '2026-12-31T23:59:59',
}])

vi.mock('@/api/projects', () => ({
  getMyProjectSubmission: vi.fn(),
  listProjects: vi.fn(),
  submitProject: vi.fn(),
}))

vi.mock('@/api/drive', () => ({ uploadDriveFile: vi.fn() }))

vi.mock('@/composables/useStudentContext', () => ({
  useStudentContext: () => ({
    courses,
    semesters,
    loading: ref(false),
    loadSemesters: vi.fn(async () => undefined),
  }),
}))

vi.mock('@/components/PageHeader.vue', () => ({
  default: defineComponent({ props: ['title'], setup: props => () => h('h1', String(props.title)) }),
}))

vi.mock('@vicons/ionicons5', () => ({
  CloudUploadOutline: defineComponent({ setup: () => () => h('span') }),
  FolderOutline: defineComponent({ setup: () => () => h('span') }),
}))

vi.mock('naive-ui', () => {
  const passthrough = (tag = 'div') => defineComponent({
    setup(_props, { slots }) {
      return () => h(tag, [slots.default?.(), slots.footer?.(), slots.icon?.()])
    },
  })
  return {
    NAlert: passthrough('aside'),
    NButton: defineComponent({
      props: ['disabled'],
      emits: ['click'],
      setup(props, { slots, emit }) {
        return () => h('button', { disabled: props.disabled, onClick: () => emit('click') }, [slots.icon?.(), slots.default?.()])
      },
    }),
    NEmpty: passthrough(),
    NIcon: passthrough('span'),
    NInput: defineComponent({
      props: ['value', 'readonly', 'placeholder'],
      emits: ['update:value'],
      setup(props, { emit }) {
        return () => h('textarea', {
          value: props.value,
          readonly: props.readonly,
          placeholder: props.placeholder,
          onInput: (event: Event) => emit('update:value', (event.target as HTMLTextAreaElement).value),
        })
      },
    }),
    NModal: defineComponent({
      props: ['show', 'title'],
      setup(props, { slots }) {
        return () => props.show ? h('section', [props.title, slots.default?.(), slots.footer?.()]) : null
      },
    }),
    NSelect: passthrough('select'),
    NSpace: passthrough(),
    NSpin: passthrough(),
    NTag: passthrough('span'),
    useMessage: () => ({ error: vi.fn(), success: vi.fn(), warning: vi.fn() }),
  }
})

const project: ProjectVO = {
  id: 301,
  name: 'Digital Story Project',
  description: JSON.stringify({
    text: 'Create a digital story.',
    artifact: { submitMode: 'file', allowedExtensions: ['pdf'] },
    rubric: [
      { dimension: 'AWARENESS', maxScore: 10 },
      { dimension: 'COMPUTING', maxScore: 20 },
    ],
  }),
  deadline: '2026-12-01T23:59:59',
  semesterId: 2,
  submissionId: 501,
  submissionStatus: 'graded',
  canResubmit: false,
  score: 26,
}

const submission: ProjectSubmissionVO = {
  id: 501,
  submissionId: 501,
  projectId: 301,
  studentId: 101,
  studentName: 'Lin Yi',
  studentNo: '2026001',
  classId: 10,
  className: 'Class 1',
  content: JSON.stringify({ note: 'Team members: Zhou Er', files: [] }),
  status: 'graded',
  canResubmit: false,
  returnReason: null,
  returnedAt: null,
  revisionCount: 0,
  score: 26,
  dimensionScores: [
    { questionId: 'project', dimension: 'AWARENESS', earnedScore: 8, maxScore: 10 },
    { questionId: 'project', dimension: 'COMPUTING', earnedScore: 18, maxScore: 20 },
  ],
  submittedAt: '2026-06-14T10:00:00',
  createdAt: '2026-06-14T10:00:00',
}

describe('student project grading detail', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(listProjects).mockResolvedValue([project])
    vi.mocked(getMyProjectSubmission).mockResolvedValue(submission)
  })

  it('auto-selects the current course and opens a graded submission read-only', async () => {
    const wrapper = mount(ProjectView)
    await flushPromises()
    await nextTick()
    await flushPromises()

    expect(listProjects).toHaveBeenCalledWith(2)
    const detailButton = wrapper.findAll('button').find(button => button.text() === '查看批改详情')
    expect(detailButton).toBeDefined()
    await detailButton?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('26 分')
    expect(wrapper.text()).toContain('信息意识 8/10')
    expect(wrapper.text()).toContain('计算思维 18/20')
    expect(wrapper.find('textarea').attributes('readonly')).toBeDefined()
    expect(wrapper.findAll('button').some(button => button.text() === '提交')).toBe(false)
    expect(submitProject).not.toHaveBeenCalled()
  })
})
