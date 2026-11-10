<template>
  <main class="phone-shell">
    <header class="app-header">
      <div class="header-title">{{ pageTitle }}</div>
      <button
        v-if="view !== 'home'"
        class="back-button"
        type="button"
        aria-label="返回上一页"
        title="返回上一页"
        @click="goBack"
      >
        <span></span>
      </button>
      <!-- <div class="mini-actions" aria-hidden="true">
        <span></span><span></span><span></span>
        <i></i>
      </div> -->
    </header>

    <section v-if="view === 'home'" class="view home-view">
      <div v-if="!activeLeave" class="empty-state">
        <p class="empty-title">当前暂无进行中的请假申请</p>
        <button class="primary-button" type="button" @click="openLeaveForm">
          发起请假
        </button>
        <img
          class="empty-image"
          src="/assets/empty-state.svg"
          alt="请假申请插画"
        />
      </div>

      <div v-else class="record-state">
        <article class="application-card" @click="openCurrentDetail">
          <div class="card-head">
            <strong>{{
              activeLeave.student_name || student?.name || "ZHOUBING"
            }}</strong>
            <span>{{
              activeLeave.student_id || student?.id || "2020090911004"
            }}</span>
          </div>
          <dl class="card-detail">
            <div>
              <dt>请假类型：</dt>
              <dd>{{ homeLeaveType }}</dd>
            </div>
            <div>
              <dt>申请时间：</dt>
              <dd>{{ formatDateTime(homeApplyTime) }}</dd>
            </div>
            <div>
              <dt>开始时间：</dt>
              <dd>{{ formatDateTime(homeStartTime) }}</dd>
            </div>
            <div>
              <dt>结束时间：</dt>
              <dd>{{ formatDateTime(homeEndTime) }}</dd>
            </div>
          </dl>
          <p class="record-status" :class="homeStatus.className">
            {{ homeStatus.text }}
          </p>
        </article>

        <button
          v-if="canStartNewLeave"
          class="text-action"
          type="button"
          @click="openLeaveForm"
        >
          +发起新的请假
        </button>
        <button
          v-if="canApplyReturn"
          class="text-action"
          type="button"
          @click="openReturnForm"
        >
          +发起返校申请
        </button>
        <p class="record-tip">{{ homeStatus.tip }}</p>
      </div>
    </section>

    <section v-if="view === 'form'" class="view form-view">
      <form @submit.prevent="submitLeave">
        <div class="form-block">
          <label class="form-row required">
            <span>是否离开晋中</span>
            <select v-model="leaveForm.isOutsideJinzhong">
              <option :value="true">是</option>
              <option :value="false">否</option>
            </select>
          </label>
          <label class="form-row required">
            <span>请假类型</span>
            <select v-model="leaveForm.leaveType">
              <option value="回家">回家</option>
              <option value="病假">病假</option>
              <option value="事假">事假</option>
              <option value="实习">实习</option>
              <option value="其他">其他</option>
            </select>
          </label>
          <div class="address-tools">
            <span>请填写离校目的地的地址(非出发地)</span>
            <button
              class="circle-action"
              type="button"
              aria-label="添加地址级别"
              @click="addAddressLevel"
            >
              +
            </button>
          </div>
          <div class="address-levels">
            <div
              v-for="(_, index) in leaveForm.addressLevels"
              :key="index"
              class="address-level"
            >
              <input
                v-model.trim="leaveForm.addressLevels[index]"
                :placeholder="`第${index + 1}级地址，如省/市/区/街道/社区`"
              />
              <button
                class="remove-address"
                type="button"
                aria-label="删除地址级别"
                @click="removeAddressLevel(index)"
              >
                -
              </button>
            </div>
          </div>
          <label class="form-row required">
            <span>所在地区</span>
            <input v-model.trim="leaveForm.region" placeholder="请选择" />
          </label>
          <label class="textarea-row required">
            <span>详细地址</span>
            <textarea
              v-model.trim="leaveForm.detailedAddress"
              rows="2"
              placeholder="请详细填写地址信息，如小区名门牌号等"
            ></textarea>
          </label>
        </div>

        <div class="form-block">
          <label class="form-row required">
            <span>开始时间</span>
            <input v-model="leaveForm.startTime" type="datetime-local" />
          </label>
          <label class="form-row required">
            <span>结束时间</span>
            <input v-model="leaveForm.endTime" type="datetime-local" />
          </label>
          <div class="duration-line">
            请假时长：<strong>{{ leaveDuration.text }}</strong>
          </div>
        </div>

        <div class="form-block">
          <label class="textarea-row required">
            <span>请假理由</span>
            <textarea
              v-model.trim="leaveForm.reason"
              maxlength="100"
              rows="4"
              placeholder="请详细填写您的请假理由，以便辅导员快速通过审核，字数不超过100字"
            ></textarea>
          </label>
        </div>

        <div class="form-block attach-block">
          <h2>附件</h2>
          <p>
            病假的同学请上传医院证明，其余请假同学请上传其他证明材料，图片单张大小不超过2M
          </p>
          <el-upload
            class="avatar-uploader"
            action=""
            :auto-upload="false"
            :file-list="uploadFileList"
            :on-change="handleAttachmentChange"
            :on-remove="handleRemove"
            accept="image/*"
            multiple
            list-type="picture-card"
          >
            <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
            <template #file="{ file }">
              <div class="el-upload-list__item is-ready">
                <img :src="file.url" class="el-upload-list__item-thumbnail" />
                <span class="el-upload-list__item-actions">
                  <span
                    class="el-upload-list__item-preview"
                    @click="previewImage(file.url, file.name)"
                  >
                    <el-icon><ZoomIn /></el-icon>
                  </span>
                  <span
                    class="el-upload-list__item-delete"
                    @click="
                      handleRemove(
                        file,
                        (uploadFileList.value || []).filter(
                          (f) => f.uid !== file.uid,
                        ),
                      )
                    "
                  >
                    <el-icon><Delete /></el-icon>
                  </span>
                </span>
              </div>
            </template>
          </el-upload>
        </div>

        <div class="form-block">
          <label class="form-row required">
            <span>本人联系电话</span>
            <input v-model.trim="leaveForm.studentPhone" placeholder="请输入" />
          </label>
          <label class="form-row">
            <span>紧急联系人姓名</span>
            <input
              v-model.trim="leaveForm.emergencyContactName"
              placeholder="请输入"
            />
          </label>
          <label class="form-row">
            <span>紧急联系人电话</span>
            <input
              v-model.trim="leaveForm.emergencyContactPhone"
              placeholder="请输入"
            />
          </label>
        </div>

        <div class="form-block flow-block">
          <h2>请假流程</h2>
          <StudentFlowList :people="approvalPeople" />
        </div>

        <div class="submit-area">
          <button class="submit-button" type="submit" :disabled="submitting">
            {{ submitting ? "提交中..." : "提交" }}
          </button>
        </div>
      </form>
    </section>

    <section v-if="view === 'return'" class="view form-view">
      <form @submit.prevent="submitReturn">
        <div class="form-block">
          <label class="form-row required">
            <span>返校时间</span>
            <input
              v-model="returnForm.returnTime"
              type="datetime-local"
              :min="returnForm.min"
              :max="returnForm.max"
            />
          </label>
          <label class="textarea-row">
            <span>返校说明</span>
            <textarea
              v-model.trim="returnForm.note"
              maxlength="100"
              rows="4"
              placeholder="请填写返校时间、交通方式或其他说明"
            ></textarea>
          </label>
        </div>
        <div class="submit-area">
          <button class="submit-button" type="submit" :disabled="submitting">
            {{ submitting ? "提交中..." : "提交返校申请" }}
          </button>
        </div>
      </form>
    </section>

    <section v-if="view === 'extension'" class="view form-view">
      <form @submit.prevent="submitExtension">
        <div class="form-block">
          <div class="duration-line">
            原结束时间：<strong>{{
              formatDateTime(activeLeave?.end_time)
            }}</strong>
          </div>
          <label class="form-row required">
            <span>续假返校时间</span>
            <input
              v-model="extensionForm.newReturnTime"
              type="datetime-local"
            />
          </label>
          <label class="textarea-row required">
            <span>续假原因</span>
            <textarea
              v-model.trim="extensionForm.reason"
              maxlength="100"
              rows="4"
              placeholder="请填写续假的具体原因，字数不超过100字"
            ></textarea>
          </label>
        </div>
        <div class="form-block flow-block">
          <h2>续假说明</h2>
          <p class="form-hint">
            提交后将由管理端审批，审批通过前请保持当前请假状态。
          </p>
        </div>
        <div class="submit-area">
          <button class="submit-button" type="submit" :disabled="submitting">
            {{ submitting ? "提交中..." : "提交续假申请" }}
          </button>
        </div>
      </form>
    </section>

    <section v-if="view === 'records'" class="view records-view">
      <div class="record-filter">
        <button
          class="period-button"
          :class="{ open: periodMenuOpen }"
          type="button"
          @click.stop="periodMenuOpen = !periodMenuOpen"
        >
          {{ periodLabels[recordsPeriod] }}<span></span>
        </button>
        <div v-if="periodMenuOpen" class="period-menu">
          <button
            type="button"
            :class="{ active: recordsPeriod === 'month' }"
            @click.stop="changeRecordsPeriod('month')"
          >
            近一个月
          </button>
          <button
            type="button"
            :class="{ active: recordsPeriod === 'all' }"
            @click.stop="changeRecordsPeriod('all')"
          >
            全部记录
          </button>
        </div>
      </div>
      <div class="records-list">
        <article
          v-for="record in records"
          :key="record.id"
          class="history-card"
          @click="showLeaveDetail(record.id)"
        >
          <div class="card-head">
            <strong>{{
              record.student_name || student?.name || "ZHOUBING"
            }}</strong>
            <span>{{
              record.student_id || student?.id || "2020090911004"
            }}</span>
          </div>
          <dl class="history-detail">
            <div>
              <dt>请假类型：</dt>
              <dd>{{ record.leave_type }}</dd>
            </div>
            <div>
              <dt>申请时间：</dt>
              <dd>{{ formatDateTime(record.created_at) }}</dd>
            </div>
            <div>
              <dt>开始时间：</dt>
              <dd>{{ formatDateTime(record.start_time) }}</dd>
            </div>
            <div>
              <dt>结束时间：</dt>
              <dd>{{ formatDateTime(record.end_time) }}</dd>
            </div>
            <div>
              <dt>请假理由：</dt>
              <dd :title="record.reason">{{ record.reason }}</dd>
            </div>
          </dl>
        </article>
      </div>
      <p v-if="!records.length" class="records-empty">暂无请假记录</p>
    </section>

    <section v-if="view === 'detail' && detail" class="view detail-view">
      <div v-if="detailAlertText" class="detail-alert">
        {{ detailAlertText }}
      </div>

      <section class="detail-section detail-student">
        <h2>
          {{
            detailLeave.student_name || student?.name || "ZHOUBING"
          }}提交的请假
        </h2>
        <p>
          {{ detailLeave.college || student?.college || "电子信息工程学院" }}
        </p>
        <strong :class="detailStatus.className">{{ detailStatus.text }}</strong>
        <div
          v-if="detailStatus.stamp"
          class="approval-stamp"
          :class="detailStatus.stamp"
          v-html="detailStatus.stampText"
        ></div>
      </section>

      <section class="detail-section info-grid">
        <div>
          <span>所在年级：</span
          ><strong>{{ detailLeave.grade || student?.grade || "2023" }}</strong>
        </div>
        <div>
          <span>所在专业：</span
          ><strong>{{
            detailLeave.major || student?.major || "电子信息工程专业"
          }}</strong>
        </div>
        <div>
          <span>所在班级：</span
          ><strong>{{
            detailLeave.class_name || student?.className || "202301班"
          }}</strong>
        </div>
      </section>

      <section class="detail-section detail-lines">
        <h3>请假时间</h3>
        <p>开始时间：<span v-html="shortDateTime(detailStartTime)"></span></p>
        <p>结束时间：<span v-html="shortDateTime(detailEndTime)"></span></p>
        <p>
          合计时长：<span>{{ detailDurationText }}</span>
        </p>
      </section>

      <section class="detail-section detail-lines">
        <h3>请假事由</h3>
        <p>{{ detailReason }}</p>
        <button class="month-record" type="button" @click="openRecords">
          <span
            >本月已经累计请假{{ detail.monthCount || 0 }}次，本年累计请假{{
              detail.yearCount || 0
            }}次</span
          >
          <i></i>
        </button>
      </section>

      <section class="detail-section detail-lines">
        <h3>申请详情</h3>
        <p>
          是否离开晋中：<span>{{
            Number(detailLeave.is_outside_jinzhong) ? "是" : "否"
          }}</span>
        </p>
        <p>
          请假类型：<span>{{
            hasDetailExtension
              ? `续假-${detailLeave.leave_type || ""}`
              : detailLeave.leave_type || ""
          }}</span>
        </p>
        <p
          v-for="(address, index) in detailAddressLevels"
          :key="`${address}-${index}`"
        >
          离校目的地{{ index + 1 }}：{{ address }}
        </p>
        <p>
          所在地区：{{
            detailAddressLevels[detailAddressLevels.length - 1] || ""
          }}
        </p>
        <p>{{ detailLeave.detailed_address || "" }}</p>
      </section>

      <section class="detail-section detail-lines">
        <h3>附件</h3>
        <div class="detail-attachments">
          <button
            v-for="(item, index) in detailAttachments"
            :key="`${item.name || '图片'}-${index}`"
            class="attachment-thumb"
            type="button"
            @click="previewAttachment(item, index)"
          >
            <img
              v-if="attachmentSrc(item)"
              :src="attachmentSrc(item)"
              :alt="item.name || `图片${index + 1}`"
            />
            <template v-else>{{ item.name || "图片" }}</template>
          </button>
        </div>
      </section>

      <section class="detail-section detail-lines">
        <h3>紧急联络</h3>
        <p>
          本人联系电话：<span>{{ detailLeave.student_phone || "" }}</span>
        </p>
        <p>
          紧急联系人姓名：<span>{{
            detailLeave.emergency_contact_name || ""
          }}</span>
        </p>
        <p>
          紧急联系人电话：<span>{{
            detailLeave.emergency_contact_phone || ""
          }}</span>
        </p>
      </section>

      <section class="detail-section detail-flow">
        <h3>请假流程</h3>
        <template
          v-for="(step, index) in detail.steps || []"
          :key="`${step.step_no}-${index}`"
        >
          <div class="detail-flow-item">
            <span class="detail-flow-dot"></span>
            <div class="detail-flow-main">
              <strong>{{ step.approver_role }}</strong>
              <p>{{ step.approver_name }}（{{ stepStatusText(step) }}）</p>
              <em v-if="step.status === 'rejected' && step.comment"
                >“{{ step.comment }}”</em
              >
            </div>
            <div class="detail-flow-avatar">{{ step.approver_code }}</div>
          </div>
          <div
            v-if="index < (detail.steps || []).length - 1"
            class="detail-flow-or"
          >
            <span>或签 一人同意即可进入下一节点</span>
          </div>
        </template>
      </section>

      <div v-if="detailActionVisible" class="detail-submit">
        <button
          class="submit-button"
          type="button"
          :disabled="submitting"
          @click="handleDetailAction"
        >
          {{ detailActionText }}
        </button>
        <button
          v-if="canExtendFromDetail"
          class="submit-button extension-button"
          type="button"
          @click="openExtensionForm"
        >
          我要续假
        </button>
      </div>
    </section>

    <div
      v-if="preview.visible"
      class="image-preview"
      @click="preview.visible = false"
    >
      <button type="button" @click="preview.visible = false">×</button>
      <div id="previewBox" @click.stop>
        <img v-if="preview.src" :src="preview.src" :alt="preview.name" />
        <template v-else>{{ preview.name || "图片" }}</template>
      </div>
    </div>

    <StudentBottomNav v-model="bottomPage" :visible="showBottomNav" />
    <StudentToast :message="toastMessage" />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { leaveApi, studentApi } from "../api";
import { useAppStore } from "../stores/appStore";
import {
  approvalLevelByMinutes,
  attachmentSrc,
  durationParts,
  durationTextFromMinutes,
  formatDateTime,
  readFileAsDataUrl,
  shortDateTime,
  toLocalInput,
} from "../utils";
import StudentBottomNav from "../components/student/StudentBottomNav.vue";
import StudentFlowList from "../components/student/StudentFlowList.vue";
import StudentToast from "../components/student/StudentToast.vue";
import { Plus, Close, ZoomIn, Delete } from "@element-plus/icons-vue";
import "../styles/student.css";

const router = useRouter();
const appStore = useAppStore();

const view = ref("home");
const bottomPage = ref("home");
const student = ref(null);
const activeLeave = ref(null);
const detail = ref(null);
const records = ref([]);
const recordsPeriod = ref("month");
const periodMenuOpen = ref(false);
const submitting = ref(false);
const toastMessage = ref("");
const selectedAttachments = ref([]);
const uploadFileList = ref([]);
const preview = reactive({ visible: false, src: "", name: "图片" });
let toastTimer = 0;

const periodLabels = {
  month: "近一个月",
  all: "全部记录",
};

const flowPeople = [
  { role: "审批人(辅导员)", name: "LIHUANYU", code: "HUANYU" },
  { role: "审批人(书记)", name: "ZHANGMIN", code: "MIN" },
  { role: "审批人(院长)", name: "WANGJIE", code: "JIE" },
];

const statusMeta = {
  leave_pending: {
    text: "请假申请流程进行中",
    className: "status-running",
    tip: "返校申请至少提前一天申请",
  },
  leave_approved: {
    text: "请假申请审核通过",
    className: "status-pass",
    tip: "返校申请至少提前一天申请",
  },
  leave_rejected: {
    text: "请假申请审核已拒绝",
    className: "status-reject",
    tip: "返校申请至少提前一天申请",
  },
  return_pending: {
    text: "返校申请流程进行中",
    className: "status-running",
    tip: "完成返校申请后才能发起新的请假",
  },
  return_approved: {
    text: "返校申请审核通过",
    className: "status-pass",
    tip: "完成返校申请后才能发起新的请假",
  },
  return_rejected: {
    text: "返校申请审核已拒绝",
    className: "status-reject",
    tip: "完成返校申请后才能发起新的请假",
  },
  extension_pending: {
    text: "续假申请审批中",
    className: "status-running",
    tip: "续假申请审批通过前请等待审批结果",
  },
  extension_approved: {
    text: "续假申请审核通过",
    className: "status-pass",
    tip: "请按新的返校时间发起返校申请",
  },
  extension_rejected: {
    text: "续假申请审核已拒绝",
    className: "status-reject",
    tip: "请按原请假结束时间返校或联系辅导员",
  },
};

const leaveForm = reactive({
  isOutsideJinzhong: true,
  leaveType: "回家",
  addressLevels: ["山西省", "晋中市", "榆次区"],
  region: "",
  detailedAddress: "",
  startTime: "",
  endTime: "",
  reason: "",
  studentPhone: "",
  emergencyContactName: "",
  emergencyContactPhone: "",
});

const returnForm = reactive({
  returnTime: "",
  note: "",
  min: "",
  max: "",
});

const extensionForm = reactive({
  newReturnTime: "",
  reason: "",
});

const pageTitle = computed(() => {
  if (view.value === "records") return "请假记录";
  if (view.value === "extension") return "续假申请";
  return "请假申请";
});

const showBottomNav = computed(() => ["home", "records"].includes(view.value));
const hasActiveExtension = computed(
  () =>
    Boolean(activeLeave.value?.extension_status) &&
    !activeLeave.value?.return_status,
);
const homeApplyTime = computed(() =>
  hasActiveExtension.value
    ? activeLeave.value?.extension_created_at || activeLeave.value?.created_at
    : activeLeave.value?.created_at,
);
const homeStartTime = computed(() =>
  hasActiveExtension.value
    ? activeLeave.value?.old_return_time || activeLeave.value?.end_time
    : activeLeave.value?.start_time,
);
const homeEndTime = computed(() =>
  hasActiveExtension.value
    ? activeLeave.value?.new_return_time || activeLeave.value?.end_time
    : activeLeave.value?.end_time,
);
const homeLeaveType = computed(() =>
  hasActiveExtension.value
    ? `续假-${activeLeave.value?.leave_type || ""}`
    : activeLeave.value?.leave_type || "回家",
);
const homeStatus = computed(
  () => statusMeta[statusKey(activeLeave.value)] || statusMeta.leave_pending,
);
const canStartNewLeave = computed(() =>
  ["leave_rejected", "return_approved"].includes(statusKey(activeLeave.value)),
);
const canApplyReturn = computed(
  () =>
    activeLeave.value &&
    activeLeave.value.leave_status === "approved" &&
    [undefined, null, "", "rejected"].includes(
      activeLeave.value.return_status,
    ) &&
    activeLeave.value.extension_status !== "pending",
);

const leaveDuration = computed(() =>
  durationParts(leaveForm.startTime, leaveForm.endTime),
);
const approvalPeople = computed(() =>
  flowPeople.slice(0, approvalLevelByMinutes(leaveDuration.value.minutes || 1)),
);

const detailLeave = computed(() => detail.value?.leave || {});
const hasDetailExtension = computed(
  () =>
    Boolean(detailLeave.value.extension_status) &&
    !detailLeave.value.return_status,
);
const detailStartTime = computed(() =>
  hasDetailExtension.value
    ? detailLeave.value.old_return_time || detailLeave.value.end_time
    : detailLeave.value.start_time,
);
const detailEndTime = computed(() =>
  hasDetailExtension.value
    ? detailLeave.value.new_return_time || detailLeave.value.end_time
    : detailLeave.value.end_time,
);
const detailReason = computed(() =>
  hasDetailExtension.value
    ? detailLeave.value.extension_reason || ""
    : detailLeave.value.reason || "",
);
const detailAddressLevels = computed(() =>
  Array.isArray(detailLeave.value.address_levels)
    ? detailLeave.value.address_levels
    : [],
);
const detailAttachments = computed(() => {
  const list = detailLeave.value.attachments;
  return Array.isArray(list) && list.length ? list : [{ name: "图片" }];
});

const detailDurationText = computed(() => {
  if (!hasDetailExtension.value)
    return durationTextFromMinutes(detailLeave.value.duration_minutes);
  const start = new Date(String(detailStartTime.value || "").replace(" ", "T"));
  const end = new Date(String(detailEndTime.value || "").replace(" ", "T"));
  return durationTextFromMinutes(
    Math.max(0, Math.round((end - start) / 60000)),
  );
});

const detailStatus = computed(() => {
  if (detailLeave.value.return_status)
    return returnStatusLabel(detailLeave.value.return_status);
  if (hasDetailExtension.value)
    return extensionStatusLabel(detailLeave.value.extension_status);
  return leaveStatusLabel(detailLeave.value.status);
});

const detailAlertText = computed(() => {
  if (detailLeave.value.return_status)
    return returnAlertText(detailLeave.value.return_status);
  if (hasDetailExtension.value)
    return extensionAlertText(detailLeave.value.extension_status);
  if (["pending", "approved"].includes(detailLeave.value.status)) {
    return detailLeave.value.status === "approved"
      ? "当前请假审批已通过，24点后将关闭进出权限，进入校门请发起返校申请"
      : "当前请假审批流程进行中，请耐心等待审批结果";
  }
  return "";
});

const canReturnFromDetail = computed(
  () =>
    detailLeave.value.status === "approved" &&
    [undefined, null, "", "rejected"].includes(
      detailLeave.value.return_status,
    ) &&
    detailLeave.value.extension_status !== "pending",
);
const canCancelFromDetail = computed(
  () => detailLeave.value.status === "pending",
);
const canExtendFromDetail = computed(() => canReturnFromDetail.value);
const detailActionVisible = computed(
  () =>
    canReturnFromDetail.value ||
    canCancelFromDetail.value ||
    canExtendFromDetail.value,
);
const detailActionText = computed(() =>
  canCancelFromDetail.value ? "撤销申请" : "返校申请",
);

function showToast(message) {
  toastMessage.value = message;
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => {
    toastMessage.value = "";
  }, 2200);
}

function statusKey(active) {
  if (!active) return "";
  if (active.return_status) return `return_${active.return_status}`;
  if (Boolean(active.extension_status) && !active.return_status)
    return `extension_${active.extension_status}`;
  return `leave_${active.leave_status}`;
}

function leaveStatusLabel(status) {
  if (status === "approved")
    return {
      text: "审批通过",
      className: "detail-status-pass",
      stamp: "pass",
      stampText: "审批<br />通过",
    };
  if (status === "rejected")
    return {
      text: "审批拒绝",
      className: "detail-status-reject",
      stamp: "reject",
      stampText: "审批<br />拒绝",
    };
  return {
    text: "审批中",
    className: "detail-status-running",
    stamp: "",
    stampText: "",
  };
}

function extensionStatusLabel(status) {
  if (status === "approved")
    return {
      text: "续假申请审批已通过",
      className: "detail-status-pass",
      stamp: "pass",
      stampText: "续假<br />通过",
    };
  if (status === "rejected")
    return {
      text: "续假申请审批已拒绝",
      className: "detail-status-reject",
      stamp: "reject",
      stampText: "续假<br />拒绝",
    };
  return {
    text: "续假申请审批中",
    className: "detail-status-running",
    stamp: "",
    stampText: "",
  };
}

function returnStatusLabel(status) {
  if (status === "approved")
    return {
      text: "返校申请审批已通过",
      className: "detail-status-pass",
      stamp: "pass",
      stampText: "返校<br />通过",
    };
  if (status === "rejected")
    return {
      text: "返校申请审批已拒绝",
      className: "detail-status-reject",
      stamp: "reject",
      stampText: "返校<br />拒绝",
    };
  return {
    text: "返校申请审批中",
    className: "detail-status-running",
    stamp: "",
    stampText: "",
  };
}

function extensionAlertText(status) {
  if (status === "approved")
    return "续假申请已通过，请按新的返校时间发起返校申请";
  if (status === "rejected")
    return "续假申请已拒绝，请按原请假结束时间返校或联系辅导员";
  return "当前续假申请审批中，请等待审批结果";
}

function returnAlertText(status) {
  if (status === "approved") return "返校申请已通过，本次请假流程已完成";
  if (status === "rejected")
    return "返校申请已拒绝，请重新提交返校申请或联系辅导员";
  return "当前返校申请审批中，请等待审批结果";
}

function stepStatusText(step) {
  if (step.status === "rejected") return "已拒绝";
  if (step.status === "approved") return step.comment || "已同意";
  if (step.status === "pending") return "待审批";
  return "待流转";
}

function setDefaultTimes() {
  const now = new Date();
  now.setMinutes(0, 0, 0);
  const end = new Date(now);
  end.setDate(end.getDate() + 2);
  end.setHours(end.getHours() + 23);
  leaveForm.startTime = toLocalInput(now);
  leaveForm.endTime = toLocalInput(end);
}

function resetLeaveForm() {
  Object.assign(leaveForm, {
    isOutsideJinzhong: true,
    leaveType: "回家",
    addressLevels: ["山西省", "晋中市", "榆次区"],
    region: "",
    detailedAddress: "",
    reason: "",
    studentPhone: "",
    emergencyContactName: "",
    emergencyContactPhone: "",
  });
  selectedAttachments.value = [];
  setDefaultTimes();
}

function addAddressLevel() {
  if (leaveForm.addressLevels.length >= 5) {
    showToast("最多添加5级目的地地址");
    return;
  }
  leaveForm.addressLevels.push("");
}

function removeAddressLevel(index) {
  leaveForm.addressLevels.splice(index, 1);
}

function openLeaveForm() {
  resetLeaveForm();
  view.value = "form";
}

function openReturnForm() {
  if (!activeLeave.value) return;
  const start = new Date(
    String(activeLeave.value.start_time).replace(" ", "T"),
  );
  const end = new Date(String(activeLeave.value.end_time).replace(" ", "T"));
  const now = new Date();
  const defaultTime = now < start ? start : now > end ? end : now;
  returnForm.min = toLocalInput(start);
  returnForm.max = toLocalInput(end);
  returnForm.returnTime = toLocalInput(defaultTime);
  returnForm.note = "";
  view.value = "return";
}

function openExtensionForm() {
  const source = detailLeave.value.id ? detailLeave.value : activeLeave.value;
  if (!source) return;
  const oldEnd = new Date(String(source.end_time).replace(" ", "T"));
  const defaultTime = new Date(oldEnd);
  defaultTime.setDate(defaultTime.getDate() + 1);
  extensionForm.newReturnTime = toLocalInput(defaultTime);
  extensionForm.reason = "";
  activeLeave.value = source;
  view.value = "extension";
}

function openRecords() {
  bottomPage.value = "records";
  loadRecords();
}

function goBack() {
  // 根据当前页面返回到正确的页面
  if (
    view.value === "form" ||
    view.value === "extension" ||
    view.value === "return"
  ) {
    view.value = "home";
    bottomPage.value = "home";
  } else if (view.value === "detail") {
    view.value = "records";
    bottomPage.value = "records";
  } else if (view.value === "records") {
    view.value = "home";
    bottomPage.value = "home";
  } else {
    // 其他情况返回上一页
    router.back();
  }
}

async function loadHome() {
  try {
    const data = await studentApi.getHome();
    student.value = data.student;
    activeLeave.value = data.active;
    return data;
  } catch (error) {
    showToast(error.message || "暂时无法读取请假状态，请检查服务");
    return null;
  }
}

async function loadRecords() {
  view.value = "records";
  bottomPage.value = "records";
  periodMenuOpen.value = false;
  try {
    const data = await studentApi.getLeaveRecords(recordsPeriod.value);
    records.value = data.records || [];
  } catch (error) {
    records.value = [];
    showToast(error.message || "暂时无法读取请假记录");
  }
}

function changeRecordsPeriod(period) {
  recordsPeriod.value = period;
  loadRecords();
}

async function showLeaveDetail(id) {
  try {
    detail.value = await leaveApi.getDetail(id);
    activeLeave.value = detail.value.leave;
    view.value = "detail";
  } catch (error) {
    showToast(error.message || "暂时无法读取详情");
  }
}

function openCurrentDetail() {
  if (activeLeave.value?.id) showLeaveDetail(activeLeave.value.id);
}

function validateLeavePayload(payload) {
  if (!payload.addressLevels.length) return "请至少添加一级目的地地址";
  if (!payload.region) return "请选择或填写所在地区";
  if (!payload.detailedAddress) return "请填写详细地址";
  if (!payload.startTime || !payload.endTime) return "请选择开始和结束时间";
  if (new Date(payload.endTime) <= new Date(payload.startTime))
    return "结束时间必须晚于开始时间";
  if (!payload.reason) return "请填写请假理由";
  if (!payload.studentPhone) return "请填写本人联系电话";
  return "";
}

async function handleAttachmentChange(uploadFile, uploadFiles) {
  const files = [...(uploadFiles || [])];
  const invalidFile = files.find((file) => !file.raw.type.startsWith("image/"));
  if (invalidFile) {
    showToast("只能上传图片附件");
    selectedAttachments.value = [];
    uploadFileList.value = [];
    return;
  }

  try {
    // 将文件转换为 base64
    const fileToBase64 = (file) => {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = (error) => reject(error);
      });
    };

    selectedAttachments.value = await Promise.all(
      files.map(async (file) => {
        const base64 = await fileToBase64(file.raw);
        return {
          name: file.name,
          size: file.size,
          type: file.raw.type,
          dataUrl: base64,
        };
      }),
    );
    uploadFileList.value = await Promise.all(
      files.map(async (file) => {
        const base64 = await fileToBase64(file.raw);
        return {
          uid: file.uid,
          name: file.name,
          url: base64,
        };
      }),
    );
  } catch (error) {
    showToast(error.message || "图片读取失败");
  }
}

function handleRemove(uploadFile, uploadFiles) {
  const files = [...(uploadFiles || [])];
  uploadFileList.value = files;
  selectedAttachments.value = files.map((file) => ({
    name: file.name,
    size: file.size,
    type: file.type,
    dataUrl: file.url,
  }));
}

async function updateSelectedAttachments(event) {
  const files = [...(event.target.files || [])];
  const invalidFile = files.find((file) => !file.type.startsWith("image/"));
  if (invalidFile) {
    showToast("只能上传图片附件");
    selectedAttachments.value = [];
    return;
  }

  try {
    selectedAttachments.value = await Promise.all(
      files.map(async (file) => ({
        name: file.name,
        size: file.size,
        type: file.type,
        dataUrl: await readFileAsDataUrl(file),
      })),
    );
  } catch (error) {
    showToast(error.message || "图片读取失败");
  }
}

async function submitLeave() {
  const addressList = leaveForm.addressLevels
    .map((item) => item.trim())
    .filter(Boolean);
  const payload = {
    isOutsideJinzhong: leaveForm.isOutsideJinzhong,
    leaveType: leaveForm.leaveType,
    addressLevels: [...addressList, leaveForm.region.trim()]
      .filter(Boolean)
      .slice(0, 5),
    region: leaveForm.region.trim(),
    detailedAddress: leaveForm.detailedAddress.trim(),
    startTime: leaveForm.startTime,
    endTime: leaveForm.endTime,
    durationMinutes: leaveDuration.value.minutes,
    reason: leaveForm.reason.trim(),
    attachments: selectedAttachments.value,
    studentPhone: leaveForm.studentPhone.trim(),
    emergencyContactName: leaveForm.emergencyContactName.trim(),
    emergencyContactPhone: leaveForm.emergencyContactPhone.trim(),
  };
  const error = validateLeavePayload(payload);
  if (error) {
    showToast(error);
    return;
  }

  submitting.value = true;
  try {
    await leaveApi.create(payload);
    showToast("提交成功");
    await loadHome();
    view.value = "home";
    bottomPage.value = "home";
  } catch (submitError) {
    showToast(submitError.message || "提交失败");
  } finally {
    submitting.value = false;
  }
}

async function submitReturn() {
  if (!activeLeave.value) return;
  if (!returnForm.returnTime) {
    showToast("请选择返校时间");
    return;
  }

  const returnTime = new Date(returnForm.returnTime);
  const start = new Date(
    String(activeLeave.value.start_time).replace(" ", "T"),
  );
  const end = new Date(String(activeLeave.value.end_time).replace(" ", "T"));
  if (returnTime < start || returnTime > end) {
    showToast(
      `返校时间必须在${formatDateTime(activeLeave.value.start_time)}至${formatDateTime(activeLeave.value.end_time)}之间`,
    );
    return;
  }

  submitting.value = true;
  try {
    await leaveApi.createReturnRequest({
      leaveId: activeLeave.value.id,
      returnTime: returnForm.returnTime,
      note: returnForm.note.trim(),
    });
    showToast("返校申请提交成功");
    const home = await loadHome();
    activeLeave.value = home?.active || activeLeave.value;
    view.value = "home";
    bottomPage.value = "home";
  } catch (error) {
    showToast(error.message || "提交失败");
  } finally {
    submitting.value = false;
  }
}

async function submitExtension() {
  const source = activeLeave.value || detailLeave.value;
  if (!source?.id) return;
  if (!extensionForm.newReturnTime) {
    showToast("请选择续假返校时间");
    return;
  }
  if (!extensionForm.reason.trim()) {
    showToast("请填写续假原因");
    return;
  }

  submitting.value = true;
  try {
    await leaveApi.createExtensionRequest({
      leaveId: source.id,
      newReturnTime: extensionForm.newReturnTime,
      reason: extensionForm.reason.trim(),
    });
    showToast("续假申请提交成功");
    await showLeaveDetail(source.id);
  } catch (error) {
    showToast(error.message || "提交失败");
  } finally {
    submitting.value = false;
  }
}

async function handleDetailAction() {
  if (!detailLeave.value.id) return;
  if (!canCancelFromDetail.value) {
    activeLeave.value = detailLeave.value;
    openReturnForm();
    return;
  }

  submitting.value = true;
  try {
    await leaveApi.cancel(detailLeave.value.id);
    showToast("撤销成功");
    await loadHome();
    view.value = "home";
    bottomPage.value = "home";
  } catch (error) {
    showToast(error.message || "撤销失败");
  } finally {
    submitting.value = false;
  }
}

function previewAttachment(item, index) {
  const src = attachmentSrc(item);
  previewImage(src, item.name || `图片${index + 1}`);
}

function previewImage(src, name = "图片") {
  preview.src = src || "";
  preview.name = name;
  preview.visible = true;
}

watch(bottomPage, (value) => {
  if (value === "home") {
    view.value = "home";
    loadHome();
  }
  if (value === "records") loadRecords();
});

onMounted(async () => {
  appStore.setPortal("student");
  try {
    const data = await studentApi.getSession();
    if (data.student?.mustChangePassword) {
      await router.replace("/student/change-password");
      return;
    }
    student.value = data.student;
    await loadHome();
  } catch {
    await router.replace("/student/login");
  }
});
</script>
