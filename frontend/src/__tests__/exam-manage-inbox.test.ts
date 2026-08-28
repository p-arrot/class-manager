import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h, nextTick, ref } from 'vue'
import ExamManage from '@/views/teacher/ExamManage.vue'
import { listExamPapers, listExams, listExamSubmissions } from '@/api/exams'
import type { ExamSubmissionVO } from '@/types/api'

const messageError = vi.fn()
const messageSuccess = vi.fn()
const activeCourseId = ref<number | null>(null)
const activeSemesterId = ref<number | null>(null)
const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/api/exams', () => ({
  createExam: vi.fn(),
  createExamPaper: vi.fn(),
  deleteExam: vi.fn(),
  gradeExamSubmission: vi.fn(),
  listExamPapers: vi.fn(),
  listExams: vi.fn(),
  listExamSubmissions: vi.fn(),
  updateExam: vi.fn(),
}))

vi.mock('@/composables/useCourseSemesterPicker', () => ({
  useCourseSemesterPicker: () => ({
    activeCourseId,
    activeSemesterId,
    courseOptions: ref([{ label: '信息科技', value: 1 }]),
    semesterOptions: ref([{ label: '2026 春', value: 2 }]),
    loadCourses: vi.fn(async () => {
      activeCourseId.value = 1
      activeSemesterId.value = 2
      await nextTick()
    }),
  }),
}))

vi.mock('@/components/PageHeader.vue', () => ({
  default: defineComponent({
    props: { title: String, subtitle: String },
    setup(props, { slots }) {
      return () => h('header', [h('h1', props.title), h('p', props.subtitle), slots.actions?.()])
    },
  }),
}))

vi.mock('@/components/MarkdownEditor.vue', () => ({
  default: defineComponent({
    props: ['modelValue'],
    emits: ['update:modelValue'],
    setup(props) {
      return () => h('textarea', String(props.modelValue ?? ''))
    },
  }),
}))

vi.mock('@vicons/ionicons5', () => ({
  AddOutline: defineComponent({ setup: () => () => h('span') }),
  CheckmarkDoneOutline: defineComponent({ setup: () => () => h('span') }),
  CreateOutline: defineComponent({ setup: () => () => h('span') }),
  TrashOutline: defineComponent({ setup: () => () => h('span') }),
}))

vi.mock('naive-ui', () => {
  const passthrough = (tag = 'div') => defineComponent({
    props: ['description', 'show', 'title'],
    setup(props, { slots }) {
      return () => h(tag, [props.title, props.description, slots.default?.(), slots.footer?.(), slots.actions?.(), slots.icon?.()])
    },
  })

  return {
    NAlert: passthrough('section'),
    NButton: defineComponent({
      props: ['disabled', 'loading', 'title', 'aria-label'],
      emits: ['click'],
      setup(props, { slots, emit }) {
        return () => h('button', {
          disabled: props.disabled || props.loading,
          title: props.title,
          'aria-label': props['aria-label'],
          onClick: () => emit('click'),
        }, [slots.icon?.(), slots.default?.()])
      },
    }),
    NCheckbox: defineComponent({
      props: ['checked'],
      emits: ['update:checked'],
      setup(_props, { slots }) {
        return () => h('label', [h('input', { type: 'checkbox' }), slots.default?.()])
      },
    }),
    NDataTable: defineComponent({
      props: ['data', 'columns', 'rowProps'],
      setup(props) {
        return () => h('table', props.data.map((row: Record<string, unknown>) => {
          const rowProps = props.rowProps?.(row) ?? {}
          return h('tr', { ...rowProps, key: String(row.id ?? row.studentId) }, props.columns.map((column: { key: string; render?: (row: unknown) => unknown }) =>
            h('td', { key: column.key }, column.render ? column.render(row) : String(row[column.key] ?? ''))))
        }))
      },
    }),
    NDatePicker: passthrough('div'),
    NEmpty: passthrough('div'),
    NForm: passthrough('form'),
    NFormItem: passthrough('label'),
    NIcon: passthrough('span'),
    NInput: defineComponent({
      props: ['value', 'disabled'],
      setup(props, { slots }) {
        return () => h('span', [String(props.value ?? ''), slots.suffix?.()])
      },
    }),
    NModal: passthrough('section'),
    NPopconfirm: defineComponent({
      setup(_props, { slots }) {
        return () => h('span', [slots.trigger?.(), slots.default?.()])
      },
    }),
    NRadio: passthrough('label'),
    NRadioGroup: passthrough('div'),
    NSelect: defineComponent({
      props: ['value', 'options', 'placeholder', 'disabled'],
      emits: ['update:value'],
      setup(props) {
        return () => h('select', { disabled: props.disabled, 'aria-label': props.placeholder })
      },
    }),
    NSpace: passthrough('div'),
    NTag: passthrough('span'),
    useMessage: () => ({ error: messageError, success: messageSuccess }),
  }
})

function submissions(): ExamSubmissionVO[] {
  return [
    {
      id: 501,
      submissionId: 501,
      examId: 301,
      studentId: 101,
      studentName: '林一',
      studentNo: '2026001',
      classId: 10,
      className: '2026级1班',
      answers: '{"q1":"A"}',
      score: null,
      status: 'submitted',
      canResubmit: true,
      returnReason: null,
      returnedAt: null,
      startedAt: '2026-06-14T09:00:00',
      revisionCount: 0,
      submittedAt: '2026-06-14T09:00:00',
      createdAt: '2026-06-14T09:00:00',
    },
    {
      id: null,
      submissionId: null,
      examId: 301,
      studentId: 102,
      studentName: '周二',
      studentNo: '2026002',
      classId: 10,
      className: '2026级1班',
      answers: null,
      score: null,
      status: 'not_submitted',
      canResubmit: false,
      returnReason: null,
      returnedAt: null,
      startedAt: null,
      revisionCount: 0,
      submittedAt: null,
      createdAt: null,
    },
  ]
}

async function mountPage() {
  vi.mocked(listExamPapers).mockResolvedValue([])
  vi.mocked(listExams).mockResolvedValue([
    {
      id: 301,
      name: '期中测试',
      startTime: '2026-06-14T09:00:00',
      endTime: '2026-06-14T10:00:00',
      weight: 1,
      semesterId: 2,
      paperId: 201,
      paperContent: JSON.stringify({
        version: 3,
        questions: [{ id: 'q1', type: 'single', stem: '选择 A', score: 10, dimensionScores: [{ dimension: 'COMPUTING', maxScore: 10 }] }],
      }),
    },
  ])
  vi.mocked(listExamSubmissions).mockResolvedValue(submissions())
  const wrapper = mount(ExamManage)
  await flushPromises()
  return wrapper
}

describe('ExamManage inbox', () => {
  beforeEach(() => {
    activeCourseId.value = null
    activeSemesterId.value = null
    vi.clearAllMocks()
  })

  it('opens the roster-first grading route', async () => {
    const wrapper = await mountPage()

    await wrapper.find('button[title="提交/批改"]').trigger('click')
    expect(routerPush).toHaveBeenCalledWith('/teacher/exams/301/submissions')
  })
})
