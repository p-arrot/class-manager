import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import TaskResult from '@/views/student/TaskResult.vue'
import { getMyTaskResult } from '@/api/tasks'
import type { TaskResultVO } from '@/types/api'

const push = vi.fn()
const back = vi.fn()
const messageError = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { taskId: '101' } }),
  useRouter: () => ({ push, back }),
}))

vi.mock('@/api/tasks', () => ({
  getMyTaskResult: vi.fn(),
}))

vi.mock('@/components/MarkdownView.vue', () => ({
  default: defineComponent({
    props: { content: String },
    setup(props) {
      return () => h('div', { 'data-testid': 'markdown-view' }, props.content)
    },
  }),
}))

vi.mock('@/components/PageHeader.vue', () => ({
  default: defineComponent({
    props: { title: String, subtitle: String },
    setup(props) {
      return () => h('header', [h('h1', props.title), h('p', props.subtitle)])
    },
  }),
}))

vi.mock('naive-ui', () => {
  const passthrough = (tag = 'div') => defineComponent({
    props: ['description'],
    setup(props, { slots }) {
      return () => h(tag, [props.description, slots.default?.(), slots.extra?.()])
    },
  })

  return {
    NAlert: passthrough('div'),
    NButton: defineComponent({
      props: ['disabled', 'loading'],
      emits: ['click'],
      setup(props, { slots, emit }) {
        return () => h('button', { disabled: props.disabled || props.loading, onClick: () => emit('click') }, [slots.icon?.(), slots.default?.()])
      },
    }),
    NCard: passthrough('section'),
    NEmpty: passthrough('div'),
    NIcon: passthrough('span'),
    NProgress: passthrough('div'),
    NSpin: passthrough('div'),
    NTag: passthrough('span'),
    useMessage: () => ({ error: messageError }),
  }
})

function baseResult(overrides: Partial<TaskResultVO> = {}): TaskResultVO {
  return {
    task: {
      id: 101,
      title: '课堂练习：条件判断',
      type: 'worksheet',
      courseId: 7,
      courseName: 'Python 入门',
      lessonId: 9,
      lessonName: '分支结构',
    },
    status: 'graded',
    submission: {
      id: 501,
      status: 'graded',
      content: '{"q1":"score >= 60"}',
      submittedAt: '2026-06-13T19:32:20',
      gradedAt: '2026-06-13T19:38:42',
      teacherComment: '思路清晰，表达可以更完整。',
    },
    questions: [
      {
        id: 'q1',
        index: 1,
        type: 'blank',
        stem: '写出及格判断表达式',
        autoGrade: true,
        referenceAnswerVisible: true,
        referenceAnswer: 'score >= 60',
      },
    ],
    answers: { q1: 'score >= 60' },
    questionResults: [
      {
        questionId: 'q1',
        correct: true,
        autoGraded: true,
        earnedScore: 8,
        maxScore: 10,
        comment: '条件表达式正确。',
        dimensionScores: [
          { dimension: 'COMPUTING', earnedScore: 8, maxScore: 10 },
        ],
      },
    ],
    dimensionSummary: [
      { dimension: 'COMPUTING', earnedScore: 8, maxScore: 10, rate: 0.8, grade: 'B' },
    ],
    ...overrides,
  }
}

async function mountPage(result?: TaskResultVO) {
  if (result) vi.mocked(getMyTaskResult).mockResolvedValueOnce(result)
  const wrapper = mount(TaskResult)
  await flushPromises()
  return wrapper
}

describe('TaskResult', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders graded result details', async () => {
    const wrapper = await mountPage(baseResult())

    expect(wrapper.text()).toContain('已批改')
    expect(wrapper.text()).toContain('8/10 分')
    expect(wrapper.text()).toContain('思路清晰，表达可以更完整。')
    expect(wrapper.text()).toContain('逐题明细')
    expect(wrapper.text()).toContain('参考答案')
    expect(wrapper.text()).toContain('条件表达式正确。')
  })

  it('renders submitted result without empty manual score claims', async () => {
    const wrapper = await mountPage(baseResult({
      status: 'submitted',
      submission: {
        id: 501,
        status: 'submitted',
        content: '{"q1":"score >= 60"}',
        submittedAt: '2026-06-13T19:32:20',
        gradedAt: null,
        teacherComment: null,
      },
      questionResults: [],
      dimensionSummary: [],
    }))

    expect(wrapper.text()).toContain('待教师批改')
    expect(wrapper.text()).toContain('当前仅显示提交内容')
    expect(wrapper.text()).toContain('未评分')
  })

  it('renders not submitted state', async () => {
    const wrapper = await mountPage(baseResult({
      status: 'not_submitted',
      submission: null,
      answers: {},
      questionResults: [],
      dimensionSummary: [],
    }))

    expect(wrapper.text()).toContain('未提交')
    expect(wrapper.text()).toContain('你还没有提交这项任务')
    expect(wrapper.text()).toContain('未作答')
  })

  it('renders special state as excluded from evaluation', async () => {
    const wrapper = await mountPage(baseResult({
      status: 'special',
      submission: {
        id: 501,
        status: 'special',
        content: '{}',
        submittedAt: '2026-06-13T19:32:20',
        gradedAt: '2026-06-13T19:38:42',
        teacherComment: '设备故障，特殊处理。',
      },
      questionResults: [],
      dimensionSummary: [],
    }))

    expect(wrapper.text()).toContain('特殊处理')
    expect(wrapper.text()).toContain('不计入评价统计')
    expect(wrapper.text()).toContain('设备故障，特殊处理。')
  })

  it('renders permission errors without result content', async () => {
    vi.mocked(getMyTaskResult).mockRejectedValueOnce(new Error('权限不足'))
    const wrapper = mount(TaskResult)
    await flushPromises()

    expect(wrapper.text()).toContain('权限不足')
    expect(wrapper.text()).not.toContain('课堂练习：条件判断')
    expect(messageError).toHaveBeenCalledWith('权限不足')
  })
})
