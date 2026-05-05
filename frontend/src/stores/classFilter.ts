import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useClassFilterStore = defineStore('classFilter', () => {
  const selectedClassId = ref<number | null>(null)

  function setClassId(id: number | null) {
    selectedClassId.value = id
  }

  function clearFilter() {
    selectedClassId.value = null
  }

  return { selectedClassId, setClassId, clearFilter }
})
