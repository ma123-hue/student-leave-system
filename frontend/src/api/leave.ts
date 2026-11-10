import { get, post } from './request';

export const leaveApi = {
  getDetail(id) {
    return get(`/api/leaves/${id}`);
  },
  create(payload) {
    return post('/api/leaves', payload);
  },
  cancel(id) {
    return post(`/api/leaves/${id}/cancel`);
  },
  createReturnRequest(payload) {
    return post('/api/return-requests', payload);
  },
  createExtensionRequest(payload) {
    return post('/api/extension-requests', payload);
  }
};
