import { defineStore } from 'pinia';

export const useAppStore = defineStore('app', {
  state: () => ({
    currentPortal: 'student'
  }),
  actions: {
    setPortal(portal) {
      this.currentPortal = portal;
    }
  }
});
