import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h, ref } from 'vue'
import TaskAnalytics from '@/views/teacher/TaskAnalytics.vue'
import { listAllClasses } from '@/api/classes'
import { getCourse } from '@/api/courses'
import { getLesson } from '@/api/lessons'
import { getSemester } from '@/api/semesters'
import { getTask, getTaskAnalytics } from '@/api/tasks'
import type { StudentTaskAnswerVO, TaskAnalyticsVO } from '@/types/api'

const push = vi.hoisted(() => vi.fn())
const back = vi.hoisted(() => vi.fn())
const messageError = vi.hoisted(() => vi.fn())
const messageSuccess = vi.hoisted(() => vi.fn())
const selectedClassId = vi.hoisted(() => ({ value: null as number | null }))
const realtimeState = vi.hoisted(() => ({ callback: null as (() => void) | null }))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { taskId: '101' }, path: '/teacher/tasks/101/analytics' }),
  useRouter: () => ({ push, back }),
}))

vi.mock('@/api/classes', () => ({ listAllClasses: vi.fn() }))
vi.mock('@/api/courses', () => ({ getCourse: vi.fn() }))
vi.mock('@/api/lessons', () => ({ getLesson: vi.fn() }))
vi.mock('@/api/semesters', () => ({ getSemester: vi.fn() }))
vi.mock('@/api/tasks', () => ({
  getTask: vi.fn(),
  getTaskAnalytics: vi.fn(),
}))

vi.mock('@/stores/classFilter', () => ({
  useClassFilterStore: () => ({
    get selectedClassId() { return selectedClassId.value },
    setClassId: (id: number | null) => { selectedClassId.value = id },
    clearFilter: () => { selectedClassId.value = null },
  }),
}))

vi.mock('@/composables/useRealtime', () => ({
  useRealtime: () => ({
    connect: vi.fn(),
    disconnect: vi.fn(),
    subscribeTask: vi.fn((_taskId: number, callback: () => void) => {
      realtimeState.callback = callback
    }),
  }),
}))

vi.mock('echarts/core', () => ({
  init: vi.fn(() => ({ setOption: vi.fn(), resize: vi.fn(), dispose: vi.fn() })),
  use: vi.fn(),
}))
vi.mock('echarts/charts', () => ({ BarChart: {}, PieChart: {} }))
vi.mock('echarts/components', () => ({ GridComponent: {}, LegendComponent: {}, TooltipComponent: {} }))
vi.mock('echarts/renderers', () => ({ CanvasRenderer: {} }))

vi.mock('@/components/MarkdownView.vue', () => ({
  default: defineComponent({
    props: { content: String },
    setup(props) {
      return () => h('div', props.content)
    },
  }),
}))

vi.mock('@/components/PageHeader.vue', () => ({
  default: defineComponent({
    props: { title: String, hint: String },
    setup(props, { slots }) {
      return () => h('header', [h('h1', props.title), h('p', props.hint), slots.actions?.()])
    },
  }),
}))

vi.mock('@vicons/ionicons5', () => ({
  ArrowBackOutline: defineComponent({ setup: () => () => h('span') }),
  CreateOutline: defineComponent({ setup: () => () => h('span') }),
  RefreshOutline: defineComponent({ setup: () => () => h('span') }),
}))

vi.mock('naive-ui', () => {
  const passthrough = (tag = 'div') => defineComponent({
    props: ['description', 'show', 'percentage'],
    setup(props, { slots }) {
      return () => h(tag, [props.description, props.percentage, slots.default?.(), slots.extra?.(), slots.icon?.()])
    },
  })

  return {
    NButton: defineComponent({
      props: ['disabled', 'loading', 'type'],
      emits: ['click'],
      setup(props, { slots, emit }) {
        return () => h('button', { disabled: props.disabled || props.loading, onClick: () => emit('click') }, [slots.icon?.(), slots.default?.()])
      },
    }),
    NDataTable: defineComponent({
      props: ['data', 'columns'],
      setup(props) {
        return () => h('table', { 'data-testid': 'inbox-table' }, props.data.map((row: StudentTaskAnswerVO) =>
          h('tr', { key: row.studentId }, props.columns.map((column: { key: string; render?: (row: StudentTaskAnswerVO) => unknown }) =>
            h('td', { key: column.key }, column.render ? column.render(row) : String((row as unknown as Record<string, unknown>)[column.key] ?? ''))))))
      },
    }),
    NEmpty: passthrough('div'),
    NIcon: passthrough('span'),
    NModal: passthrough('section'),
    NProgress: passthrough('div'),
    NSelect: defineComponent({
      props: ['value', 'options', 'placeholder', 'loading'],
      emits: ['update:value'],
      setup(props) {
        return () => h('select', { 'aria-label': props.placeholder })
      },
    }),
    NSpin: passthrough('div'),
    NTag: passthrough('span'),
    useMessage: () => ({ error: messageError, success: messageSuccess }),
    useThemeVars: () => ref({
      primaryColor: '#2563eb',
      successColor: '#16a34a',
      warningColor: '#a16207',
      railColor: '#d6d3cc',
      textColor2: '#44403c',
      borderColor: '#d6d3cc',
      dividerColor: '#e7e5e0',
    }),
  }
})

function analytics(): TaskAnalyticsVO {
  return {
    taskId: 101,
    title: '条件判断练习',
    type: 'worksheet',
    totalStudents: 2,
    submittedCount: 1,
    gradedCount: 0,
    specialCount: 0,
    notSubmittedCount: 1,
    submissionRate: 50,
    accuracyRate: 0,
    questionCount: 0,
    autoQuestionCount: 0,
    manualQuestionCount: 0,
    selectedClassId: null,
    classScopes: [{ id: 10, grade: '2026', name: '1班', studentCount: 2 }],
    questions: [],
    submissions: [
      {
        submissionId: 501,
        studentId: 101,
        studentName: '林一',
        studentNo: '2026101',
        classId: 10,
        className: '2026级1班',
        status: 'submitted',
        content: '{"q1":"A"}',
        submittedAt: '2026-06-14T09:00:00',
      },
      {
        submissionId: null,
        studentId: 102,
        studentName: '周二',
        studentNo: '2026102',
        classId: 10,
        className: '2026级1班',
        status: 'not_submitted',
        content: null,
        submittedAt: null,
      },
    ],
  }
}

async function mountPage() {
  vi.mocked(getTask).mockResolvedValue({ id: 101, lessonId: 201, title: '条件判断练习', type: 'worksheet', description: '', formSchema: '{}', deadline: null, createdAt: null } as never)
  vi.mocked(getLesson).mockResolvedValue({ id: 201, semesterId: 301, title: '第 1 课' } as never)
  vi.mocked(getSemester).mockResolvedValue({ id: 301, courseId: 401, name: '2026 春' } as never)
  vi.mocked(getCourse).mockResolvedValue({ id: 401, name: '信息科技', classIds: [10] } as never)
  vi.mocked(listAllClasses).mockResolvedValue([{ id: 10, grade: '2026', name: '1班' }] as never)
  vi.mocked(getTaskAnalytics).mockResolvedValue(analytics())
  const wrapper = mount(TaskAnalytics)
  await flushPromises()
  return wrapper
}

describe('TaskAnalytics inbox', () => {
  beforeEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
    selectedClassId.value = null
    realtimeState.callback = null
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
      callback(0)
      return 1
    })
  })

  it('renders all expected students including not submitted rows', async () => {
    const wrapper = await mountPage()

    expect(wrapper.text()).toContain('批改收件箱')
    expect(wrapper.text()).toContain('2 名应完成学生')
    expect(wrapper.find('[data-testid="inbox-table"]').text()).toContain('林一')
    expect(wrapper.find('[data-testid="inbox-table"]').text()).toContain('周二')
    expect(wrapper.find('[data-testid="inbox-table"]').text()).toContain('待批改')
    expect(wrapper.find('[data-testid="inbox-table"]').text()).toContain('未提交')
    expect(wrapper.find('[data-testid="inbox-table"]').text()).toContain('尚未提交')
  })

  it('opens grading at the selected submission and disables missing submissions', async () => {
    const wrapper = await mountPage()
    const rows = wrapper.findAll('tr')
    const submittedRow = rows.find(row => row.text().includes('林一'))
    const missingRow = rows.find(row => row.text().includes('周二'))

    await submittedRow?.find('button').trigger('click')

    expect(push).toHaveBeenCalledWith('/teacher/grading/101?submissionId=501')
    expect(missingRow?.find('button').attributes('disabled')).toBeDefined()
  })

  it('merges a burst of realtime events into one analytics refresh', async () => {
    vi.useFakeTimers()
    const wrapper = await mountPage()

    expect(getTaskAnalytics).toHaveBeenCalledTimes(1)
    expect(realtimeState.callback).toBeTypeOf('function')

    for (let index = 0; index < 30; index += 1) realtimeState.callback?.()
    expect(getTaskAnalytics).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(500)
    await flushPromises()

    expect(getTaskAnalytics).toHaveBeenCalledTimes(2)
    wrapper.unmount()
    vi.useRealTimers()
  })
})
