import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import ProjectSubmissionModal from '@/components/project/ProjectSubmissionModal.vue'
import type { ProjectSubmissionRow } from '@/types/project'

vi.mock('@vicons/ionicons5', () => ({
  CloudDownloadOutline: defineComponent({ setup: () => () => h('span') }),
  EyeOutline: defineComponent({ setup: () => () => h('span') }),
}))

vi.mock('naive-ui', () => {
  const passthrough = (tag = 'div') => defineComponent({
    props: ['description', 'show', 'title'],
    setup(props, { slots }) {
      return () => h(tag, [props.title, props.description, slots.default?.(), slots.icon?.(), slots.suffix?.()])
    },
  })

  return {
    NButton: defineComponent({
      props: ['disabled', 'loading'],
      emits: ['click'],
      setup(props, { slots, emit }) {
        return () => h('button', { disabled: props.disabled || props.loading, onClick: () => emit('click') }, [slots.icon?.(), slots.default?.()])
      },
    }),
    NEmpty: passthrough('div'),
    NIcon: passthrough('span'),
    NInput: defineComponent({
      props: ['value'],
      emits: ['update:value'],
      setup(props, { slots }) {
        return () => h('label', [h('input', { value: String(props.value ?? '') }), slots.suffix?.()])
      },
    }),
    NModal: passthrough('section'),
    NSpace: passthrough('div'),
  }
})

function rows(): ProjectSubmissionRow[] {
  return [
    {
      id: 501,
      submissionId: 501,
      projectId: 301,
      teamId: null,
      studentId: 101,
      studentName: '林一',
      studentNo: '2026001',
      classId: 10,
      className: '2026级1班',
      content: '{"note":"我和周二合作完成","files":[{"id":11,"name":"作品.zip"}]}',
      status: 'submitted',
      score: null,
      submittedAt: '2026-06-14T09:00:00',
      createdAt: '2026-06-14T09:00:00',
      parsed: {
        note: '我和周二合作完成',
        files: [{ id: 11, name: '作品.zip' }],
      },
    },
    {
      id: null,
      submissionId: null,
      projectId: 301,
      teamId: null,
      studentId: 102,
      studentName: '周二',
      studentNo: '2026002',
      classId: 10,
      className: '2026级1班',
      content: null,
      status: 'not_submitted',
      score: null,
      submittedAt: null,
      createdAt: null,
      parsed: {
        note: '',
        files: [],
      },
    },
  ]
}

describe('ProjectSubmissionModal inbox', () => {
  it('renders submitted and not submitted rows without scoring missing submissions', () => {
    const wrapper = mount(ProjectSubmissionModal, {
      props: {
        show: true,
        title: '项目提交情况',
        rows: rows(),
        rubric: [{ dimension: 'COMPUTING', maxScore: 10 }],
        getScore: () => 0,
      },
    })

    expect(wrapper.text()).toContain('林一')
    expect(wrapper.text()).toContain('周二')
    expect(wrapper.text()).toContain('待评分')
    expect(wrapper.text()).toContain('未提交')
    expect(wrapper.text()).toContain('尚未提交项目作品')
    expect(wrapper.text()).toContain('我和周二合作完成')
    expect(wrapper.text()).toContain('作品.zip')
    expect(wrapper.findAll('button').filter(button => button.text().includes('保存评分'))).toHaveLength(1)
  })
})
