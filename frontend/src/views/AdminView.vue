<template>
  <main class="phone-shell">
    <header class="app-header">
      <div class="header-title" @click="handleTitleClick">{{ pageTitle }}</div>
      <button class="back-button" type="button" aria-label="返回上一页" title="返回上一页" @click="goBack">
        <span></span>
      </button>
      <button class="logout-button" type="button" title="退出管理端" @click="logoutAdmin">退出</button>
      <!-- <div class="mini-actions" aria-hidden="true">
        <span></span><span></span><span></span>
        <i></i>
      </div> -->
    </header>

    <template v-if="page === 'approval'">
      <div class="notice-bar">{{ noticeText }}</div>

      <nav class="top-tabs">
        <button
          v-for="tab in approvalTabs"
          :key="tab.type"
          class="top-tab"
          :class="{ active: approvalType === tab.type }"
          type="button"
          @click="changeApprovalType(tab.type)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <section class="search-panel">
        <label class="search-box">
          <span></span>
          <input v-model.trim="approvalSearch" placeholder="请输入姓名/学号模糊查询" @input="debouncedLoadApprovals" />
        </label>
        <div v-if="approvalType === 'overtime'" class="filter-row">
          <label v-for="item in overtimeFilters" :key="item.value">
            <input
              type="checkbox"
              :value="item.value"
              :checked="overtimeFilter === item.value"
              @change="changeOvertimeFilter(item.value)"
            />{{ item.label }}
          </label>
        </div>
      </section>

      <section v-if="approvalType !== 'ranking'" class="admin-list">
        <ApprovalCard
          v-for="row in approvalRows"
          :key="`${row.approval_kind || approvalType}-${row.id}-${row.return_id || ''}-${row.extension_id || ''}`"
          :row="row"
          :type="approvalType"
          :format-date-time="formatDateTime"
          @open="showDetail"
          @approve="approveCurrent"
        />
        <div v-if="!approvalRows.length" class="approval-card">暂无数据</div>
      </section>

      <section v-else class="ranking-list">
        <table class="ranking-table">
          <thead>
            <tr>
              <th>排名</th>
              <th></th>
              <th>姓名</th>
              <th>月排名</th>
              <th>年度排名</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in approvalRows" :key="row.student_id || index">
              <td><span :class="index < 3 ? 'rank-medal' : 'rank-num'">{{ index + 1 }}</span></td>
              <td><div class="small-avatar">HAIBO</div></td>
              <td>{{ row.student_name }}</td>
              <td>{{ row.month_count }}</td>
              <td>{{ row.year_count }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <!-- <div class="loading-line">
        <span></span>{{ loading ? '加载中...' : '加载完成' }}
      </div> -->
    </template>

    <section v-if="page === 'detail' && detail" class="admin-detail">
      <div class="detail-alert">当前请假审批已通过。24点后将关闭进出权限。进入校门请发起返校申请</div>

      <section class="detail-section detail-student">
        <h2>{{ leave.student_name || '' }}提交的请假</h2>
        <p>{{ leave.college || '电子信息工程学院' }}</p>
        <strong id="detailStatus">{{ detailStatusText }}</strong>
      </section>

      <section class="detail-section info-grid">
        <div><span>所在年级：</span><strong>{{ leave.grade || '' }}</strong></div>
        <div><span>所在专业：</span><strong>{{ leave.major || '' }}</strong></div>
        <div><span>所在班级：</span><strong>{{ leave.class_name || '' }}</strong></div>
      </section>

      <section class="detail-section detail-lines">
        <h3>请假时间</h3>
        <p>开始时间：<span>{{ formatDateTime(detailStartTime) }}</span></p>
        <p>结束时间：<span>{{ formatDateTime(detailEndTime) }}</span></p>
        <p>合计时长：<span>{{ detailDurationText }}</span></p>
      </section>

      <section class="detail-section detail-lines">
        <h3>请假事由</h3>
        <p>{{ detailReason }}</p>
        <button class="month-record" type="button">
          <span>本月已经累计请假{{ detail.monthCount || 0 }}次，本年累计请假{{ detail.yearCount || 0 }}次</span>
          <i></i>
        </button>
      </section>

      <section class="detail-section detail-lines">
        <h3>申请详情</h3>
        <p>是否离开晋中：<span>{{ Number(leave.is_outside_jinzhong) ? '是' : '否' }}</span></p>
        <p>请假类型：<span>{{ hasExtensionStatus ? `续假-${leave.leave_type || ''}` : leave.leave_type || '' }}</span></p>
        <template v-for="(address, index) in addressLevels" :key="`${address}-${index}`">
          <p>离校目的地{{ index + 1 }}</p>
          <p>所在地区：{{ address }}</p>
        </template>
        <p>{{ leave.detailed_address || '' }}</p>
      </section>

      <section class="detail-section detail-lines">
        <h3>附件</h3>
        <div class="detail-attachments">
          <button
            v-for="(item, index) in attachments"
            :key="`${item.name || '图片'}-${index}`"
            class="attachment-thumb"
            type="button"
            @click="previewAttachment(item)"
          >
            <img v-if="attachmentSrc(item)" :src="attachmentSrc(item)" :alt="item.name || `图片${index + 1}`" />
            <template v-else>{{ item.name || '图片' }}</template>
          </button>
        </div>
      </section>

      <section class="detail-section detail-lines">
        <h3>紧急联络</h3>
        <p>本人联系电话：<span>{{ leave.student_phone || '' }}</span></p>
        <p>紧急联系人姓名：<span>{{ leave.emergency_contact_name || '' }}</span></p>
        <p>紧急联系人电话：<span>{{ leave.emergency_contact_phone || '' }}</span></p>
      </section>

      <section class="detail-section detail-flow">
        <h3>请假流程</h3>
        <template v-for="(step, index) in detail.steps || []" :key="`${step.step_no}-${index}`">
          <div class="detail-flow-item">
            <span class="detail-flow-dot"></span>
            <div class="detail-flow-main">
              <strong>{{ step.approver_role }}</strong>
              <p>{{ step.approver_name }}（{{ stepStatusText(step) }}）</p>
            </div>
            <div class="detail-flow-avatar">{{ step.approver_code }}</div>
          </div>
          <div v-if="index < (detail.steps || []).length - 1" class="detail-flow-or">
            <span>或签 一人同意即可进入下一节点</span>
          </div>
        </template>
      </section>

      <section v-if="!detailReadonly" class="admin-actions">
        <button class="action-blue" type="button" @click="handleDetailAction('approve-leave')">{{ approveLeaveText }}</button>
        <button class="action-red" type="button" @click="handleDetailAction('reject-leave')">拒绝请假</button>
        <button class="action-blue" type="button" @click="handleDetailAction('approve-return')">同意返校</button>
        <button class="action-red" type="button" @click="handleDetailAction('reject-return')">拒绝返校</button>
        <button class="action-blue" type="button" @click="handleDetailAction('approve-extension')">{{ approveExtensionText }}</button>
        <button class="action-red" type="button" @click="handleDetailAction('reject-extension')">驳回延期返校</button>
      </section>
    </section>

    <section v-if="page === 'role'" class="role-panel">
      <div class="role-header">
        <h2>请选择角色</h2>
        <p>切换将变更审批流程和管辖范围</p>
      </div>
      <button
        v-for="item in roles"
        :key="item"
        class="role-option"
        :class="{ active: currentRole === item }"
        type="button"
        @click="saveRole(item)"
      >
        <span>{{ item }}</span>
        <i></i>
      </button>
    </section>

    <section v-if="page === 'history'" class="history-panel">
      <div class="history-filters">
        <label v-for="item in historyScopes" :key="item.value">
          <input
            type="checkbox"
            :value="item.value"
            :checked="historyScope === item.value"
            @change="changeHistoryScope(item.value)"
          />{{ item.label }}
        </label>
      </div>
      <label class="history-search">
        <span></span>
        <input v-model.trim="historySearch" placeholder="请输入姓名/学号模糊查询" @input="debouncedLoadHistory" />
      </label>
      <div class="history-list">
        <article
          v-for="row in historyRows"
          :key="row.id"
          class="history-card"
          @click="showDetail(row.id, { readonly: true })"
        >
          <div class="card-head">
            <strong>{{ row.student_name }}</strong>
            <span>{{ row.student_id }}</span>
          </div>
          <dl class="history-detail">
            <div><dt>请假类型：</dt><dd>{{ row.leave_type }}</dd></div>
            <div><dt>申请时间：</dt><dd>{{ formatDateTime(row.created_at) }}</dd></div>
            <div><dt>开始时间：</dt><dd>{{ formatDateTime(row.start_time) }}</dd></div>
            <div><dt>结束时间：</dt><dd>{{ formatDateTime(row.end_time) }}</dd></div>
            <div><dt>请假理由：</dt><dd>{{ row.reason }}</dd></div>
          </dl>
        </article>
        <article v-if="!historyRows.length" class="history-card">暂无审批记录</article>
      </div>
    </section>

    <section v-if="page === 'stats'" class="stats-panel">
      <article class="stats-card">
        <div class="stats-card-title">
          <h2>离校目的地人数分布</h2>
          <button
            class="province-toggle"
            :class="{ open: provinceExpanded }"
            type="button"
            :aria-expanded="String(provinceExpanded)"
            @click="provinceExpanded = !provinceExpanded"
          >
            <span>{{ provinceExpanded ? '收起' : '全部' }}</span>
            <i></i>
          </button>
        </div>
        <div class="province-stats-list">
          <div v-for="item in visibleProvinces" :key="item.province" class="province-stat-row">
            <span>{{ item.province }}</span>
            <strong>{{ Number(item.student_count || 0) }}人</strong>
          </div>
        </div>
        <p class="stats-note">• 统计当前未返校的学生离校目的地省份</p>
      </article>

      <article class="stats-card">
        <h2>学生请假类型分布</h2>
        <VChart class="leave-type-pie" :option="leaveTypePieOption" autoresize />
      </article>

      <article class="stats-card">
        <h2>各学院请假/返校人数</h2>
        <div class="stats-switch">
          <button :class="{ active: collegeMode === 'leave' }" type="button" @click="collegeMode = 'leave'">请假申请</button>
          <button :class="{ active: collegeMode === 'return' }" type="button" @click="collegeMode = 'return'">返校申请</button>
        </div>
        <StatsBarList :rows="statsData?.colleges || []" label-key="college" :value-key="collegeMode === 'return' ? 'return_count' : 'leave_count'" />
      </article>
    </section>

    <AdminBottomNav v-model="bottomPage" :visible="page !== 'detail'" />
    <AdminToast :message="toastMessage" />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import { LegendComponent, TooltipComponent } from 'echarts/components';
import VChart from 'vue-echarts';
import { adminApi } from '../api';
import { useAppStore } from '../stores/appStore';
import { attachmentSrc, durationTextFromMinutes, formatDateTime } from '../utils';
import AdminBottomNav from '../components/admin/AdminBottomNav.vue';
import AdminToast from '../components/admin/AdminToast.vue';
import ApprovalCard from '../components/admin/ApprovalCard.vue';
import StatsBarList from '../components/admin/StatsBarList.vue';
import '../styles/admin.css';

use([CanvasRenderer, PieChart, LegendComponent, TooltipComponent]);

const appStore = useAppStore();
const router = useRouter();

const approvalTabs = [
  { type: 'leave', label: '请假审批' },
  { type: 'return', label: '返校审批' },
  { type: 'overtime', label: '超时未归' },
  { type: 'ranking', label: '次数排名' }
];

const overtimeFilters = [
  { value: 'all', label: '全部' },
  { value: 'returned', label: '已申请返校' },
  { value: 'not_returned', label: '未申请返校' }
];

const historyScopes = [
  { value: 'all', label: '全部' },
  { value: 'mine', label: '我审批的' },
  { value: 'cc', label: '抄送我的' }
];

const page = ref('approval');
const bottomPage = ref('approval');
const pageHistory = ref([]);
const approvalType = ref('leave');
const approvalSearch = ref('');
const overtimeFilter = ref('all');
const approvalRows = ref([]);
const approvalCounts = ref({});
const loading = ref(false);
const detail = ref(null);
const detailReadonly = ref(false);
const roles = ref(['辅导员', '学院分管副院长', '学院党委副书记']);
const currentRole = ref('');
const historyScope = ref('all');
const historySearch = ref('');
const historyRows = ref([]);
const statsData = ref(null);
const collegeMode = ref('leave');
const provinceExpanded = ref(false);
const toastMessage = ref('');

let toastTimer = 0;
let approvalTimer = 0;
let historyTimer = 0;

const leave = computed(() => detail.value?.leave || {});
const hasExtensionStatus = computed(() => Boolean(leave.value.extension_status) && !leave.value.return_status);
const addressLevels = computed(() => (Array.isArray(leave.value.address_levels) ? leave.value.address_levels : []));
const attachments = computed(() => {
  const list = leave.value.attachments;
  return Array.isArray(list) && list.length ? list : [{ name: '图片' }];
});

const pageTitle = computed(() => {
  if (page.value === 'detail') return '请假申请详情';
  if (page.value === 'role') return '角色切换';
  if (page.value === 'history') return '请假记录';
  if (page.value === 'stats') return '数据统计';
  return '请假审批';
});

const noticeText = computed(() => {
  if (approvalType.value === 'return') return `返校待审批${approvalCounts.value.returnPending || 0}人`;
  if (approvalType.value === 'overtime') return `超过请假/延期返校结束时间还未返校${approvalCounts.value.overtimeCount || 0}人`;
  if (approvalType.value === 'ranking') return '默认以月排名次数倒序';
  return `请假待审批${approvalCounts.value.leavePending || 0}人`;
});

const detailStatusText = computed(() => {
  if (hasExtensionStatus.value) {
    if (leave.value.extension_status === 'approved') return '续假申请已通过';
    if (leave.value.extension_status === 'rejected') return '续假申请已拒绝';
    return '续假申请待审批';
  }
  if (leave.value.status === 'approved') return '请假审批已通过';
  if (leave.value.status === 'rejected') return '请假审批已拒绝';
  return '请假流程进行中';
});

const detailStartTime = computed(() => (hasExtensionStatus.value ? leave.value.old_return_time : leave.value.start_time));
const detailEndTime = computed(() => (hasExtensionStatus.value ? leave.value.new_return_time : leave.value.end_time));
const detailReason = computed(() => (hasExtensionStatus.value ? leave.value.extension_reason || '' : leave.value.reason || ''));
const detailDurationText = computed(() => {
  if (!hasExtensionStatus.value) return durationTextFromMinutes(leave.value.duration_minutes);
  const start = new Date(String(detailStartTime.value || '').replace(' ', 'T'));
  const end = new Date(String(detailEndTime.value || '').replace(' ', 'T'));
  return durationTextFromMinutes(Math.max(0, Math.round((end - start) / 60000)));
});

const needsNextApproval = computed(() => {
  const pendingStep = (detail.value?.steps || []).find(step => step.status === 'pending');
  const total = leave.value.extension_status === 'pending'
    ? leave.value.extension_approval_level || 1
    : leave.value.approval_level || 1;
  return pendingStep && Number(pendingStep.step_no || 1) < Number(total);
});

const approveLeaveText = computed(() => (needsNextApproval.value ? '同意并上报' : '同意请假'));
const approveExtensionText = computed(() => (needsNextApproval.value ? '同意续假并上报' : '同意延期返校'));

const sortedProvinces = computed(() => [...(statsData.value?.provinces || [])].sort((a, b) => Number(b.student_count || 0) - Number(a.student_count || 0)));
const visibleProvinces = computed(() => (provinceExpanded.value ? sortedProvinces.value : sortedProvinces.value.slice(0, 12)));
const leaveTypePieOption = computed(() => {
  const data = (statsData.value?.types || []).map(item => ({
    name: item.leave_type || '未知类型',
    value: Number(item.count || 0)
  }));

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c}人 ({d}%)'
    },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: {
        color: '#5d6572',
        fontSize: 12
      }
    },
    series: [
      {
        name: '请假类型',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '43%'],
        avoidLabelOverlap: true,
        label: {
          formatter: '{b}\n{d}%',
          color: '#273242',
          fontSize: 12
        },
        labelLine: {
          length: 10,
          length2: 8
        },
        data
      }
    ]
  };
});

function showToast(message) {
  toastMessage.value = message;
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => {
    toastMessage.value = '';
  }, 2000);
}

function rememberPage(nextPage) {
  if (page.value && page.value !== nextPage) {
    pageHistory.value.push(page.value);
  }
  page.value = nextPage;
  if (nextPage !== 'detail') bottomPage.value = nextPage;
}

function stepStatusText(step) {
  if (step.status === 'approved') return step.comment || '已同意';
  if (step.status === 'rejected') return step.comment || '已拒绝';
  if (step.status === 'pending') return '待审批';
  return '待流转';
}

function previewAttachment(item) {
  const src = attachmentSrc(item);
  if (!src) {
    showToast(item.name || '暂无附件预览');
    return;
  }

  const preview = window.open(src, '_blank');
  if (preview) {
    preview.opener = null;
  }
}

async function loadApprovals(options = {}) {
  if (options.remember !== false) rememberPage('approval');
  else {
    page.value = 'approval';
    bottomPage.value = 'approval';
  }
  loading.value = true;

  try {
    const data = await adminApi.getApprovals({
      type: approvalType.value,
      search: approvalSearch.value,
      filter: overtimeFilter.value
    });
    approvalRows.value = data.rows || [];
    approvalCounts.value = data.counts || {};
  } catch (error) {
    showToast(error.message || '读取失败');
  } finally {
    loading.value = false;
  }
}

function debouncedLoadApprovals() {
  window.clearTimeout(approvalTimer);
  approvalTimer = window.setTimeout(() => loadApprovals(), 250);
}

function changeApprovalType(type) {
  approvalType.value = type;
  loadApprovals();
}

function changeOvertimeFilter(filter) {
  overtimeFilter.value = filter;
  loadApprovals();
}

async function approveCurrent(row) {
  const kind = row.approval_kind || approvalType.value;
  const id = kind === 'extension' ? row.extension_id : kind === 'return' ? row.return_id : row.id;

  try {
    const data = kind === 'extension'
      ? await adminApi.approveExtension(id)
      : kind === 'return'
        ? await adminApi.approveReturn(id)
        : await adminApi.approveLeave(id);
    showToast(data.message || '审批成功');
    await loadApprovals();
  } catch (error) {
    showToast(error.message || '审批失败');
  }
}

async function showDetail(id, options = {}) {
  if (options.remember !== false) rememberPage('detail');
  detailReadonly.value = Boolean(options.readonly);

  try {
    detail.value = await adminApi.getLeaveDetail(id);
  } catch (error) {
    showToast(error.message || '读取详情失败');
  }
}

async function handleDetailAction(action) {
  const currentLeave = leave.value;
  if (!currentLeave.id) return;

  try {
    let data = null;
    if (action === 'approve-leave') data = await adminApi.approveLeave(currentLeave.id);
    if (action === 'reject-leave') data = await adminApi.rejectLeave(currentLeave.id, { comment: '不同意请假' });
    if (action === 'approve-return') {
      if (!currentLeave.return_id) throw new Error('暂无待审批返校申请');
      data = await adminApi.approveReturn(currentLeave.return_id);
    }
    if (action === 'reject-return') {
      if (!currentLeave.return_id) throw new Error('暂无待审批返校申请');
      data = await adminApi.rejectReturn(currentLeave.return_id, { comment: '不同意返校' });
    }
    if (action === 'approve-extension') {
      if (!currentLeave.extension_id) throw new Error('暂无待审批延期返校申请');
      data = await adminApi.approveExtension(currentLeave.extension_id);
    }
    if (action === 'reject-extension') {
      if (!currentLeave.extension_id) throw new Error('暂无待审批延期返校申请');
      data = await adminApi.rejectExtension(currentLeave.extension_id, { comment: '不同意延期返校' });
    }
    showToast(data?.message || '操作成功');
    await showDetail(currentLeave.id, { remember: false, readonly: detailReadonly.value });
  } catch (error) {
    showToast(error.message || '操作失败');
  }
}

async function loadRole(options = {}) {
  if (options.remember !== false) rememberPage('role');
  else {
    page.value = 'role';
    bottomPage.value = 'role';
  }

  try {
    const data = await adminApi.getRole();
    currentRole.value = data.role;
    roles.value = data.roles || roles.value;
  } catch (error) {
    showToast(error.message || '读取角色失败');
  }
}

async function saveRole(role) {
  try {
    const data = await adminApi.updateRole({ role });
    currentRole.value = data.role;
    showToast('角色切换成功');
  } catch (error) {
    showToast(error.message || '角色切换失败');
  }
}

async function loadHistory(options = {}) {
  if (options.remember !== false) rememberPage('history');
  else {
    page.value = 'history';
    bottomPage.value = 'history';
  }

  try {
    const data = await adminApi.getHistory({
      scope: historyScope.value,
      search: historySearch.value
    });
    historyRows.value = data.rows || [];
  } catch (error) {
    showToast(error.message || '读取审批记录失败');
  }
}

function debouncedLoadHistory() {
  window.clearTimeout(historyTimer);
  historyTimer = window.setTimeout(() => loadHistory(), 250);
}

function changeHistoryScope(scope) {
  historyScope.value = scope;
  loadHistory();
}

async function loadStatistics(options = {}) {
  if (options.remember !== false) rememberPage('stats');
  else {
    page.value = 'stats';
    bottomPage.value = 'stats';
  }

  try {
    statsData.value = await adminApi.getStatistics();
  } catch (error) {
    showToast(error.message || '读取统计失败');
  }
}

function goBack() {
  const previousPage = pageHistory.value.pop();
  if (previousPage) {
    restorePage(previousPage);
    return;
  }

  if (window.history.length > 1) {
    window.history.back();
    return;
  }

  loadApprovals({ remember: false });
}

async function logoutAdmin() {
  try {
    await adminApi.logout();
  } finally {
    await router.replace('/admin/login');
  }
}

function restorePage(targetPage) {
  if (targetPage === 'role') loadRole({ remember: false });
  else if (targetPage === 'history') loadHistory({ remember: false });
  else if (targetPage === 'stats') loadStatistics({ remember: false });
  else loadApprovals({ remember: false });
}

function handleTitleClick() {
  if (page.value === 'detail') {
    pageHistory.value = [];
    loadApprovals({ remember: false });
  }
}

watch(bottomPage, value => {
  if (value === 'approval') loadApprovals();
  if (value === 'stats') loadStatistics();
  if (value === 'role') loadRole();
  if (value === 'history') loadHistory();
});

onMounted(() => {
  appStore.setPortal('admin');
  loadApprovals({ remember: false });
});
</script>
