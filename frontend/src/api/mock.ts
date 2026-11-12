const STORAGE_KEY = 'student-leave-demo-state-v1';

const student = {
  id: '2024000001',
  studentId: '2024000001',
  student_id: '2024000001',
  name: '张三',
  studentName: '张三',
  student_name: '张三',
  gender: '男',
  college: '信息工程学院',
  grade: '2024级',
  major: '软件工程',
  className: '软件工程2401班',
  class_name: '软件工程2401班',
  mustChangePassword: false,
  must_change_password: false
};

const initialLeaves = [
  {
    id: 1001,
    student_id: '2024000001',
    student_name: '张三',
    leave_type: '事假',
    address_levels: ['山西省', '太原市', '迎泽区'],
    detailed_address: '青年路示例地址',
    start_time: '2026-11-14T09:00:00',
    end_time: '2026-11-16T18:00:00',
    duration_minutes: 3420,
    reason: '参加校外产品实践活动',
    student_phone: '13800000000',
    emergency_contact_name: '家长',
    emergency_contact_phone: '13900000000',
    approval_level: 2,
    leave_status: 'approved',
    status: 'approved',
    created_at: '2026-11-10T10:20:00',
    return_status: null,
    extension_status: null,
    attachments: []
  },
  {
    id: 1002,
    student_id: '2024000001',
    student_name: '张三',
    leave_type: '病假',
    address_levels: ['山西省', '晋中市', '榆次区'],
    detailed_address: '校医院',
    start_time: '2026-10-20T08:00:00',
    end_time: '2026-10-21T18:00:00',
    duration_minutes: 2040,
    reason: '感冒就医休息',
    student_phone: '13800000000',
    emergency_contact_name: '家长',
    emergency_contact_phone: '13900000000',
    approval_level: 1,
    leave_status: 'ended',
    status: 'ended',
    created_at: '2026-10-19T20:10:00',
    return_status: 'approved',
    return_time: '2026-10-21T17:30:00',
    extension_status: null,
    attachments: []
  }
];

const adminPending = [
  {
    id: 2001,
    student_id: '2024000012',
    student_name: '李明',
    leave_type: '病假',
    start_time: '2026-11-13T08:00:00',
    end_time: '2026-11-15T18:00:00',
    created_at: '2026-11-12T09:15:00',
    reason: '发烧需回家休息',
    address_levels: ['山西省', '太原市', '小店区'],
    detailed_address: '平阳路示例地址',
    duration_minutes: 3480,
    approval_level: 2,
    step_no: 1,
    status: 'pending',
    leave_status: 'pending',
    student_phone: '13800000012',
    emergency_contact_name: '家长',
    emergency_contact_phone: '13900000012',
    attachments: []
  },
  {
    id: 2002,
    student_id: '2024000026',
    student_name: '王芳',
    leave_type: '实习',
    start_time: '2026-11-18T08:00:00',
    end_time: '2026-11-22T18:00:00',
    created_at: '2026-11-12T10:30:00',
    reason: '参加企业开放日与实习面试',
    address_levels: ['北京市', '北京市', '海淀区'],
    detailed_address: '中关村软件园',
    duration_minutes: 6360,
    approval_level: 3,
    step_no: 1,
    status: 'pending',
    leave_status: 'pending',
    student_phone: '13800000026',
    emergency_contact_name: '家长',
    emergency_contact_phone: '13900000026',
    attachments: []
  }
];

function initialState() {
  return {
    currentRole: '辅导员',
    leaves: structuredClone(initialLeaves),
    adminPending: structuredClone(adminPending),
    history: [
      { ...initialLeaves[1], record_status: 'approved', record_time: '2026-10-21T18:00:00' },
      {
        id: 1901,
        student_id: '2024000008',
        student_name: '赵丽',
        leave_type: '事假',
        reason: '参加创新创业竞赛',
        start_time: '2026-10-12T09:00:00',
        end_time: '2026-10-13T18:00:00',
        created_at: '2026-10-10T16:30:00',
        record_status: 'approved',
        record_time: '2026-10-10T17:05:00'
      },
      {
        id: 1902,
        student_id: '2024000031',
        student_name: '陈宇',
        leave_type: '回家',
        reason: '家庭事务',
        start_time: '2026-09-28T08:00:00',
        end_time: '2026-10-03T18:00:00',
        created_at: '2026-09-26T11:20:00',
        record_status: 'rejected',
        record_time: '2026-09-26T14:10:00'
      }
    ]
  };
}

function loadState() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? JSON.parse(saved) : initialState();
  } catch {
    return initialState();
  }
}

function saveState(state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function delay(value) {
  return new Promise(resolve => window.setTimeout(() => resolve(structuredClone(value)), 180));
}

function detailFor(leave) {
  const approved = ['approved', 'ended'].includes(leave.status || leave.leave_status);
  const rejected = (leave.status || leave.leave_status) === 'rejected';
  const steps = Array.from({ length: Number(leave.approval_level || 1) }, (_, index) => ({
    step_no: index + 1,
    approver_role: index === 0 ? '审批人(辅导员)' : index === 1 ? '审批人(学院党委副书记)' : '审批人(学院分管副院长)',
    approver_name: index === 0 ? '演示辅导员' : '演示审批人',
    status: approved ? 'approved' : rejected ? 'rejected' : index === 0 ? 'pending' : 'waiting',
    comment: approved ? '已同意' : rejected ? '申请信息不完整' : '',
    decided_at: approved || rejected ? '2026-11-12T11:00:00' : null,
    created_at: leave.created_at
  }));
  return { leave, steps, monthCount: 1, yearCount: 4 };
}

function matchesSearch(row, search = '') {
  const keyword = String(search).trim().toLowerCase();
  return !keyword || `${row.student_name} ${row.student_id}`.toLowerCase().includes(keyword);
}

function parseId(url) {
  const match = url.match(/\/(\d+)(?:\/|$)/);
  return match ? Number(match[1]) : 0;
}

function findLeave(state, id) {
  return [...state.leaves, ...state.adminPending, ...state.history].find(item => Number(item.id) === Number(id));
}

export async function mockRequest(url, options = {}) {
  const state = loadState();
  const method = String(options.method || 'GET').toUpperCase();
  const body = options.body || {};
  const params = options.params || {};

  if (url === '/api/student/login') {
    if (body.studentId !== '2024000001' || body.password !== '123456') throw new Error('演示账号或密码错误');
    return delay({ student });
  }
  if (url === '/api/student/session') return delay({ student });
  if (url === '/api/student/change-password') return delay({ message: '演示模式已跳过真实改密' });
  if (url === '/api/student/logout') return delay({ message: '已退出演示账号' });
  if (url === '/api/student-home') {
    const active = state.leaves.find(item => !['ended', 'cancelled', 'rejected'].includes(item.leave_status));
    return delay({ student, active: active || null });
  }
  if (url === '/api/leave-records') {
    const records = [...state.leaves].sort((a, b) => String(b.created_at).localeCompare(String(a.created_at)));
    return delay({ records });
  }
  if (url === '/api/leaves' && method === 'POST') {
    const nextId = Date.now();
    const leave = {
      id: nextId,
      student_id: student.id,
      student_name: student.name,
      leave_type: body.leaveType,
      address_levels: body.addressLevels || [],
      detailed_address: body.detailedAddress,
      start_time: body.startTime,
      end_time: body.endTime,
      duration_minutes: body.durationMinutes,
      reason: body.reason,
      attachments: body.attachments || [],
      student_phone: body.studentPhone,
      emergency_contact_name: body.emergencyContactName,
      emergency_contact_phone: body.emergencyContactPhone,
      approval_level: body.durationMinutes > 4320 ? 3 : body.durationMinutes > 1440 ? 2 : 1,
      leave_status: 'pending',
      status: 'pending',
      created_at: new Date().toISOString(),
      return_status: null,
      extension_status: null
    };
    state.leaves.unshift(leave);
    saveState(state);
    return delay({ leave, message: '提交成功' });
  }
  if (/^\/api\/leaves\/\d+$/.test(url) && method === 'GET') {
    const leave = findLeave(state, parseId(url));
    if (!leave) throw new Error('未找到请假记录');
    return delay(detailFor(leave));
  }
  if (/^\/api\/leaves\/\d+\/cancel$/.test(url)) {
    const leave = findLeave(state, parseId(url));
    if (leave) {
      leave.leave_status = 'cancelled';
      leave.status = 'cancelled';
      saveState(state);
    }
    return delay({ message: '撤销成功' });
  }
  if (url === '/api/return-requests') {
    const leave = findLeave(state, body.leaveId);
    if (leave) {
      leave.return_id = Date.now();
      leave.return_status = 'pending';
      leave.return_time = body.returnTime;
      leave.return_note = body.note;
      saveState(state);
    }
    return delay({ message: '返校申请提交成功' });
  }
  if (url === '/api/extension-requests') {
    const leave = findLeave(state, body.leaveId);
    if (leave) {
      leave.extension_id = Date.now();
      leave.extension_status = 'pending';
      leave.old_return_time = leave.end_time;
      leave.new_return_time = body.newReturnTime;
      leave.extension_reason = body.reason;
      leave.extension_approval_level = 2;
      saveState(state);
    }
    return delay({ message: '续假申请提交成功' });
  }

  if (url === '/api/admin/auth/login') {
    if (body.adminId !== 'admin001' || body.password !== '123456') throw new Error('演示账号或密码错误');
    return delay({ admin: { adminId: 'admin001', name: '演示辅导员', role: state.currentRole } });
  }
  if (url === '/api/admin/auth/session') return delay({ admin: { adminId: 'admin001', name: '演示辅导员', role: state.currentRole } });
  if (url === '/api/admin/auth/logout') return delay({ message: '已退出演示账号' });
  if (url === '/api/admin/role' && method === 'GET') {
    return delay({ role: state.currentRole, roles: ['辅导员', '学院分管副院长', '学院党委副书记'] });
  }
  if (url === '/api/admin/role' && method === 'POST') {
    state.currentRole = body.role;
    saveState(state);
    return delay({ role: state.currentRole });
  }
  if (url === '/api/admin/statistics') {
    return delay({
      provinces: [
        { province: '山西省', student_count: 126 }, { province: '河南省', student_count: 58 },
        { province: '河北省', student_count: 43 }, { province: '山东省', student_count: 36 },
        { province: '陕西省', student_count: 29 }, { province: '安徽省', student_count: 24 }
      ],
      types: [
        { leave_type: '事假', count: 46 }, { leave_type: '病假', count: 31 },
        { leave_type: '回家', count: 18 }, { leave_type: '实习', count: 12 }
      ],
      colleges: [
        { college: '信息工程学院', leave_count: 38, return_count: 34 },
        { college: '经济管理学院', leave_count: 31, return_count: 28 },
        { college: '智能工程学院', leave_count: 24, return_count: 22 },
        { college: '艺术设计学院', leave_count: 14, return_count: 13 }
      ],
      ratio: { stayCount: 1893, leaveCount: 107 }
    });
  }
  if (url === '/api/admin/history') {
    return delay({ rows: state.history.filter(row => matchesSearch(row, params.search)) });
  }
  if (url === '/api/admin/approvals') {
    let rows = [];
    if (params.type === 'ranking') {
      rows = [
        { student_id: '2024000012', student_name: '李明', leave_count: 6 },
        { student_id: '2024000026', student_name: '王芳', leave_count: 5 },
        { student_id: '2024000008', student_name: '赵丽', leave_count: 4 },
        { student_id: '2024000031', student_name: '陈宇', leave_count: 3 }
      ];
    } else if (params.type === 'return') {
      rows = state.leaves.filter(item => item.return_status === 'pending').map(item => ({ ...item, approval_kind: 'return', return_id: item.return_id }));
    } else if (params.type === 'overtime') {
      rows = state.history.slice(0, 2).map(item => ({ ...item, approval_kind: 'overtime' }));
    } else {
      rows = state.adminPending;
    }
    rows = rows.filter(row => matchesSearch(row, params.search));
    return delay({
      rows,
      records: rows,
      total: rows.length,
      counts: { leavePending: state.adminPending.length, returnPending: state.leaves.filter(item => item.return_status === 'pending').length, overtimeCount: 2 }
    });
  }
  if (/^\/api\/admin\/leaves\/\d+$/.test(url)) {
    const leave = findLeave(state, parseId(url));
    if (!leave) throw new Error('未找到申请记录');
    return delay(detailFor(leave));
  }
  if (/^\/api\/admin\/(leaves|returns|extensions)\/\d+\/(approve|reject)$/.test(url)) {
    const id = parseId(url);
    const approved = url.endsWith('/approve');
    const leave = findLeave(state, id) || state.adminPending.find(item => Number(item.id) === id);
    if (leave) {
      if (url.includes('/returns/')) leave.return_status = approved ? 'approved' : 'rejected';
      else if (url.includes('/extensions/')) leave.extension_status = approved ? 'approved' : 'rejected';
      else {
        leave.status = approved ? 'approved' : 'rejected';
        leave.leave_status = leave.status;
        state.adminPending = state.adminPending.filter(item => Number(item.id) !== id);
      }
      state.history.unshift({ ...leave, record_status: approved ? 'approved' : 'rejected', record_time: new Date().toISOString() });
      saveState(state);
    }
    return delay({ message: approved ? '审批已通过' : '审批已拒绝' });
  }

  throw new Error(`演示模式暂未支持该操作：${method} ${url}`);
}

