package com.example.studentleave.enums;

public enum LeaveStatusEnum {

    PENDING_COUNSELOR("pending", "待导员审批", 1, false),
    PENDING_DEPUTY_SECRETARY("pending", "待副书记审批", 2, false),
    PENDING_DEPUTY_DEAN("pending", "待副院长审批", 3, false),
    LEAVE_APPROVED("approved", "请假通过", 0, false),
    APPROVAL_REJECTED("rejected", "审批拒绝", 0, true),
    EXTENDING("pending", "续假中", 0, false),
    PENDING_RETURN("approved", "待销假", 0, false),
    FINISHED("ended", "已完结", 0, true),
    CANCELLED("cancelled", "已取消", 0, true);

    private final String dbStatus;
    private final String description;
    private final Integer stepNo;
    private final boolean terminal;

    LeaveStatusEnum(String dbStatus, String description, Integer stepNo, boolean terminal) {
        this.dbStatus = dbStatus;
        this.description = description;
        this.stepNo = stepNo;
        this.terminal = terminal;
    }

    public String getDbStatus() {
        return dbStatus;
    }

    public String getDescription() {
        return description;
    }

    public Integer getStepNo() {
        return stepNo;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public static LeaveStatusEnum pendingByStepNo(Integer stepNo) {
        if (Integer.valueOf(2).equals(stepNo)) {
            return PENDING_DEPUTY_SECRETARY;
        }
        if (Integer.valueOf(3).equals(stepNo)) {
            return PENDING_DEPUTY_DEAN;
        }
        return PENDING_COUNSELOR;
    }
}
