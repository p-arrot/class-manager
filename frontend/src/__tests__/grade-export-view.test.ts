import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h, nextTick, ref } from 'vue'
import GradeExport from '@/views/teacher/GradeExport.vue'
import { exportSemesterStats, getSemesterStatsPreview } from '@/api/stats'
import type { SemesterStatsPreviewRow } from '@/types/api'

const messageError = vi.fn()
const messageSuccess = vi.fn()
const activeCourseId = ref<number | null>(null)
const activeSemesterId = ref<number | null>(null)

vi.mock('@/api/stats', () => ({
  getSemesterStatsPreview: vi.fn(),
  exportSemesterStats: vi.fn(),
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

vi.mock('@vicons/ionicons5', () => ({
  DownloadOutline: defineComponent({ setup: () => () => h('span') }),
  RefreshOutline: defineComponent({ setup: () => () => h('span') }),
}))

vi.mock('naive-ui', () => {
  const passthrough = (tag = 'div') => defineComponent({
    props: ['description', 'show'],
    setup(props, { slots }) {
      return () => h(tag, [props.description, slots.default?.()])
    },
  })

  return {
    NAlert: passthrough('section'),
    NButton: defineComponent({
      props: ['disabled', 'loading'],
      emits: ['click'],
      setup(props, { slots, emit }) {
        return () => h('button', { disabled: props.disabled || props.loading, onClick: () => emit('click') }, [slots.icon?.(), slots.default?.()])
      },
    }),
    NDataTable: defineComponent({
      props: ['data'],
      setup(props) {
        return () => h('table', { 'data-testid': 'grade-table' }, props.data.map((row: SemesterStatsPreviewRow) =>
          h('tr', { key: row.studentId }, [
            h('td', row.className || ''),
            h('td', row.studentNo),
            h('td', row.studentName),
            h('td', row.totalGrade || ''),
            h('td', row.remark || '可导出'),
          ])))
      },
    }),
    NEmpty: passthrough('div'),
    NIcon: passthrough('span'),
    NSelect: defineComponent({
      props: ['value', 'options', 'placeholder', 'disabled'],
      emits: ['update:value'],
      setup(props) {
        return () => h('select', { disabled: props.disabled, 'aria-label': props.placeholder })
      },
    }),
    NSpin: passthrough('div'),
    NTag: passthrough('span'),
    useMessage: () => ({ error: messageError, success: messageSuccess }),
  }
})

function rows(): SemesterStatsPreviewRow[] {
  return [
    {
      studentId: 101,
      className: '2026级1班',
      studentNo: '2026001',
      studentName: '林一',
      awareness: 86,
      computing: 90,
      digitalLearn: 82,
      responsibility: 88,
      processScore: 85.5,
      examScore: 90,
      projectScore: null,
      resultScore: 90,
      totalScore: 87.8,
      totalGrade: 'B',
      remark: '',
    },
    {
      studentId: 102,
      className: '2026级1班',
      studentNo: '2026002',
      studentName: '周二',
      awareness: null,
      computing: null,
      digitalLearn: null,
      responsibility: null,
      processScore: null,
      examScore: 76,
      projectScore: null,
      resultScore: 76,
      totalScore: 76,
      totalGrade: 'B',
      remark: '缺平时任务成绩',
    },
  ]
}

async function mountPage() {
  vi.mocked(getSemesterStatsPreview).mockResolvedValue(rows())
  const wrapper = mount(GradeExport)
  await flushPromises()
  return wrapper
}

describe('GradeExport', () => {
  beforeEach(() => {
    activeCourseId.value = null
    activeSemesterId.value = null
    vi.clearAllMocks()
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:grade-export'),
      revokeObjectURL: vi.fn(),
    })
  })

  it('loads semester preview and renders summary', async () => {
    const wrapper = await mountPage()

    expect(getSemesterStatsPreview).toHaveBeenCalledWith(2)
    expect(wrapper.text()).toContain('学生数')
    expect(wrapper.text()).toContain('可生成总评')
    expect(wrapper.text()).toContain('缺失数据')
    expect(wrapper.text()).toContain('87.8')
    expect(wrapper.text()).toContain('缺平时任务成绩：1 人')
    expect(wrapper.find('[data-testid="grade-table"]').text()).toContain('林一')
  })

  it('exports current semester as excel', async () => {
    vi.mocked(exportSemesterStats).mockResolvedValue(new Blob(['excel']))
    const click = vi.fn()
    const originalCreateElement = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
      const element = originalCreateElement(tagName)
      if (tagName === 'a') element.click = click
      return element
    })
    const wrapper = await mountPage()

    await wrapper.findAll('button').find(button => button.text().includes('导出 Excel'))?.trigger('click')

    expect(exportSemesterStats).toHaveBeenCalledWith(2)
    expect(click).toHaveBeenCalled()
    expect(messageSuccess).toHaveBeenCalledWith('导出成功')
  })
})
