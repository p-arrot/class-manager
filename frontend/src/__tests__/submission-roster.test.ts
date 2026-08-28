import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import SubmissionRoster from '@/components/grading/SubmissionRoster.vue'

describe('SubmissionRoster', () => {
  it('groups the complete roster by class and selects a student', async () => {
    const wrapper = mount(SubmissionRoster, {
      props: {
        rows: [
          { studentId: 1, studentName: '林一', studentNo: '2026001', classId: 10, className: '2026级1班', submissionId: 101, status: 'submitted', submittedAt: '2026-06-01T09:00:00', score: null, revisionCount: 0 },
          { studentId: 2, studentName: '周二', studentNo: '2026002', classId: 10, className: '2026级1班', submissionId: null, status: 'not_submitted', submittedAt: null, score: null, revisionCount: 0 },
          { studentId: 3, studentName: '吴三', studentNo: '2026003', classId: 11, className: '2026级2班', submissionId: 103, status: 'graded', submittedAt: '2026-06-01T09:10:00', score: 88, revisionCount: 1 },
        ],
      },
      attachTo: document.body,
    })

    expect(wrapper.text()).toContain('2026级1班')
    expect(wrapper.text()).toContain('2026级2班')
    expect(wrapper.text()).toContain('未提交')
    expect(wrapper.text()).toContain('已批改')

    const row = wrapper.findAll('tr').find(item => item.text().includes('林一'))
    await row?.trigger('click')
    expect(wrapper.emitted('select')?.[0]?.[0]).toMatchObject({ studentId: 1 })
    wrapper.unmount()
  })
})
