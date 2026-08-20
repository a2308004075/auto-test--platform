import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 项目状态管理
 */
export const useProjectStore = defineStore('project', () => {
  const currentProjectId = ref<number>(0)
  const currentProjectName = ref<string>('')

  function setCurrentProject(id: number, name: string) {
    currentProjectId.value = id
    currentProjectName.value = name
  }

  function clearCurrentProject() {
    currentProjectId.value = 0
    currentProjectName.value = ''
  }

  return {
    currentProjectId,
    currentProjectName,
    setCurrentProject,
    clearCurrentProject,
  }
})
