import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 项目状态管理
 */
export const useProjectStore = defineStore('project', () => {
  const currentProjectId = ref<number | null>(null)
  const currentProjectName = ref<string>('')

  function setCurrentProject(id: number, name: string) {
    currentProjectId.value = id
    currentProjectName.value = name
  }

  function clearCurrentProject() {
    currentProjectId.value = null
    currentProjectName.value = ''
  }

  return {
    currentProjectId,
    currentProjectName,
    setCurrentProject,
    clearCurrentProject,
  }
})
