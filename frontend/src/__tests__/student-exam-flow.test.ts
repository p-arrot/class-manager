import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, nextTick, ref } from 'vue'
import ExamView from '@/views/student/ExamView.vue'
import { getMyExamSubmission, listExams, saveExamDraft, startExam, submitExam } from '@/api/exams'
import type { ExamSubmissionVO, ExamVO } from '@/types/api'

const messageError = vi.fn()
const messageSuccess = vi.fn()
const courses = ref([{ id: 1, name: '信息科技' }])
const semesters = ref([{
  id: 2,
  courseId: 1,
  name: '2026 春',
  startTime: '2026-02-01T00:00:00',
  endTime: '2026-07-31T23:59:59',
}])

vi.mock('@/api/exams', () => ({
  getMyExamSubmission: vi.fn(),
  listExams: vi.fn(),
  saveExamDraft: vi.fn(),
  startExam: vi.fn(),
  submitExam: vi.fn(),
}))

vi.mock('@/composables/useStudentContext', () => ({
  useStudentContext: () => ({
    courses,
    semesters,
    loading: ref(false),
    loadSemesters: vi.fn(async () => undefined),
  }),
}))

vi.mock('@/components/PageHeader.vue', () => ({
  default: defineComponent({
    props: ['title'],
    setup: props => () => h('h1', String(props.title)),
  }),
}))

vi.mock('@/components/WorksheetRenderer.vue', () => ({
  default: defineComponent({ setup: () => () => h('div', 'worksheet') }),
}))

vi.mock('naive-ui', () => {
  const passthrough = (tag = 'div') => defineComponent({
    setup(_props, { slots }) {
      return () => h(tag, [slots.default?.(), slots.footer?.()])
    },
  })
  return {
    NAlert: passthrough('aside'),
    NButton: defineComponent({
      props: ['disabled'],
      emits: ['click'],
      setup(props, { slots, emit }) {
        return () => h('button', { disabled: props.disabled, onClick: () => emit('click') }, slots.default?.())
      },
    }),
    NEmpty: passthrough(),
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
        return () => props.show ? h('section', { class: 'exam-modal' }, [props.title, slots.default?.(), slots.footer?.()]) : null
      },
    }),
    NSelect: defineComponent({
      props: ['value', 'options', 'placeholder'],
      emits: ['update:value'],
      setup: (props, { emit }) => () => h('select', {
        value: props.value ?? '',
        'aria-label': props.placeholder,
        onChange: (event: Event) => emit('update:value', Number((event.target as HTMLSelectElement).value)),
      }, (props.options ?? []).map((option: { value: number; label: string }) =>
        h('option', { value: option.value }, option.label))),
    }),
    NSpace: passthrough(),
    NSpin: passthrough(),
    NTag: passthrough('span'),
    useMessage: () => ({ error: messageError, success: messageSuccess }),
  }
})

function exam(status: ExamVO['submissionStatus']): ExamVO {
  return {
    id: 301,
    semesterId: 2,
    paperId: 201,
    name: '期中测试',
    startTime: '2026-06-14T09:00:00',
    endTime: '2026-06-14T11:00:00',
    weight: 1,
    paperContent: '',
    submissionStatus: status,
    returnReason: status === 'returned' ? '请补充解题过程' : null,
    score: status === 'graded' ? 88 : null,
  }
}

function submission(status: ExamSubmissionVO['status']): ExamSubmissionVO {
  return {
    id: 501,
    submissionId: 501,
    examId: 301,
    studentId: 101,
    studentName: '林一',
    studentNo: '2026001',
    classId: 10,
    className: '2026级1班',
    answers: 'old answer',
    score: status === 'graded' ? 88 : null,
    status,
    canResubmit: status === 'returned',
    returnReason: status === 'returned' ? '请补充解题过程' : null,
    returnedAt: status === 'returned' ? '2026-06-14T09:30:00' : null,
    startedAt: '2026-06-14T09:00:00',
    revisionCount: status === 'returned' ? 1 : 0,
    submittedAt: '2026-06-14T09:20:00',
    createdAt: '2026-06-14T09:00:00',
  }
}

async function mountPage(item: ExamVO) {
  vi.mocked(listExams).mockResolvedValue([item])
  const wrapper = mount(ExamView)
  const selects = wrapper.findAll('select')
  await selects[0]?.setValue('1')
  await flushPromises()
  await selects[1]?.setValue('2')
  await vi.advanceTimersByTimeAsync(0)
  await flushPromises()
  await nextTick()
  await vi.advanceTimersByTimeAsync(0)
  await flushPromises()
  return wrapper
}

describe('student exam lifecycle', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-14T10:00:00'))
    vi.clearAllMocks()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  it('shows the return reason and saves changed answers after one second', async () => {
    vi.mocked(startExam).mockResolvedValue(submission('returned'))
    vi.mocked(saveExamDraft).mockResolvedValue({ ...submission('in_progress'), answers: 'new answer' })
    const wrapper = await mountPage(exam('returned'))

    expect(wrapper.text()).toContain('退回原因：请补充解题过程')
    await wrapper.findAll('button').find(button => button.text() === '继续答题')?.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('教师退回：请补充解题过程')

    await wrapper.find('textarea').setValue('new answer')
    await vi.advanceTimersByTimeAsync(999)
    expect(saveExamDraft).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(1)
    await flushPromises()
    expect(saveExamDraft).toHaveBeenCalledWith(301, { answers: 'new answer' })
    expect(wrapper.text()).toContain('草稿已保存')
    wrapper.unmount()
  })

  it('opens a graded result as read-only without starting another attempt', async () => {
    vi.mocked(getMyExamSubmission).mockResolvedValue(submission('graded'))
    const wrapper = await mountPage(exam('graded'))

    await wrapper.findAll('button').find(button => button.text() === '查看结果')?.trigger('click')
    await flushPromises()

    expect(getMyExamSubmission).toHaveBeenCalledWith(301)
    expect(startExam).not.toHaveBeenCalled()
    expect(wrapper.find('textarea').attributes('readonly')).toBeDefined()
    expect(wrapper.text()).toContain('已批改 · 88 分')
    expect(wrapper.text()).not.toContain('草稿已保存')
    expect(wrapper.findAll('button').some(button => button.text() === '正式提交')).toBe(false)
    expect(submitExam).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
