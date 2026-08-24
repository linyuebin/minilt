import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const name = ref(localStorage.getItem('admin_name') || '')

  async function signIn(username, password) {
    const data = await api.login(username, password)
    token.value = data.token
    name.value = data.username || username
    localStorage.setItem('admin_token', token.value)
    localStorage.setItem('admin_name', name.value)
  }

  function signOut() {
    token.value = ''
    name.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_name')
  }

  return { token, name, signIn, signOut }
})
