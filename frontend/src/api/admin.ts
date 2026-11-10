import { get, post } from './request';

export const adminApi = {
  login(payload) {
    return post('/api/admin/auth/login', payload);
  },
  getSession() {
    return get('/api/admin/auth/session');
  },
  logout() {
    return post('/api/admin/auth/logout');
  },
  getRole() {
    return get('/api/admin/role');
  },
  updateRole(payload) {
    return post('/api/admin/role', payload);
  },
  getStatistics() {
    return get('/api/admin/statistics');
  },
  getHistory(params) {
    return get('/api/admin/history', params);
  },
  getApprovals(params) {
    return get('/api/admin/approvals', params);
  },
  getLeaveDetail(id) {
    return get(`/api/admin/leaves/${id}`);
  },
  approveLeave(id) {
    return post(`/api/admin/leaves/${id}/approve`);
  },
  rejectLeave(id, payload) {
    return post(`/api/admin/leaves/${id}/reject`, payload);
  },
  approveReturn(id) {
    return post(`/api/admin/returns/${id}/approve`);
  },
  rejectReturn(id, payload) {
    return post(`/api/admin/returns/${id}/reject`, payload);
  },
  approveExtension(id) {
    return post(`/api/admin/extensions/${id}/approve`);
  },
  rejectExtension(id, payload) {
    return post(`/api/admin/extensions/${id}/reject`, payload);
  }
};
