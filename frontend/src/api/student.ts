import { get, post } from './request';

export const studentApi = {
  login(payload) {
    return post('/api/student/login', payload);
  },
  getSession() {
    return get('/api/student/session');
  },
  changePassword(payload) {
    return post('/api/student/change-password', payload);
  },
  logout() {
    return post('/api/student/logout');
  },
  getHome() {
    return get('/api/student-home');
  },
  getLeaveRecords(period) {
    return get('/api/leave-records', { period });
  }
};
