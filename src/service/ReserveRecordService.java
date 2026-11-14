package service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dao.ReserveRecordDao;
import dao.ResourceDao;
import entity.CampusResource;
import entity.ReserveRecord;
import entity.ReserveStatus;
import entity.User;
import exception.BusinessException;
import exception.InvalidReserveStatusException;
import exception.ParameterEmptyException;
import exception.PermissionDeniedException;
import exception.ResourceNotFoundException;
import exception.ResourceOccupiedException;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public class ReserveRecordService {
    private static final DateTimeFormatter RESERVE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReserveRecordDao records;
    private final ResourceDao resources;

    public ReserveRecordService(ReserveRecordDao records, ResourceDao resources) {
        this.records = records;
        this.resources = resources;
    }

    public void requireStudentAccess(User user) {
        ensureStudent(user);
    }

    public void requireAdminReserveAccess(User user) {
        if (!"admin".equals(user.getRole())) {
            throw new PermissionDeniedException("权限不足，仅管理员可以审核或查询全部预约。");
        }
    }

    public LocalDateTime parseReserveTime(String value) {
        try {
            return LocalDateTime.parse(value.trim(), RESERVE_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new BusinessException("时间格式错误，请使用 yyyy-MM-dd HH:mm。");
        }
    }

    public Result<ReserveRecord> submitReserve(User user, String resourceId,
            LocalDateTime start, LocalDateTime end, String remark) {
        ensureStudent(user);
        CampusResource resource = resources.findById(resourceId);
        if (resource == null) {
            throw new ResourceNotFoundException("资源不存在，无法提交预约。");
        }
        if (!start.isBefore(end)) {
            throw new BusinessException("开始时间必须早于结束时间。");
        }
        if (end.isBefore(LocalDateTime.now())) {
            throw new BusinessException("结束时间不能早于当前时间。");
        }
        if (hasPassConflict(resourceId, start, end)) {
            throw new ResourceOccupiedException("预约时间冲突：该资源已有已通过的预约。");
        }
        if (!"AVAILABLE".equals(resource.getStatus())) {
            throw new ResourceOccupiedException("资源当前不可预约。");
        }

        String recordId = generateReserveId();
        ReserveRecord record = new ReserveRecord(recordId, resourceId, user.getUsername(),
                LocalDateTime.now(), start, end, ReserveStatus.PENDING, remark);
        records.add(record);
        try {
            saveReserves();
        } catch (BusinessException e) {
            records.remove(record);
            throw e;
        }
        return Result.success("预约申请提交成功，记录 ID：" + recordId, record);
    }

    public Result<List<ReserveRecord>> queryMyReserves(User user) {
        ensureStudent(user);
        return collectReserveQuery(records.findAll().stream()
                .filter(record -> user.getUsername().equals(record.getApplicantUsername())),
                "查询我的预约成功。", "当前没有预约记录。");
    }

    public Result<ReserveRecord> cancelReserve(User user, String recordId) {
        ensureStudent(user);
        ReserveRecord record = Optional.ofNullable(records.findById(recordId))
                .orElseThrow(() -> new BusinessException("预约记录不存在，无法取消。"));
        if (!user.getUsername().equals(record.getApplicantUsername())) {
            throw new PermissionDeniedException("只能取消本人的预约。");
        }
        requireTransition(record.getStatus(), ReserveStatus.CANCEL);

        boolean wasPass = record.getStatus() == ReserveStatus.PASS;
        CampusResource resource = null;
        if (wasPass) {
            resource = resources.findById(record.getResourceId());
            if (resource == null) {
                throw new ResourceNotFoundException("关联资源不存在，无法取消已通过预约。");
            }
        }
        boolean release = wasPass && records.findAll().stream().noneMatch(other -> other != record
                && record.getResourceId().equals(other.getResourceId())
                && other.getStatus() == ReserveStatus.PASS);
        updateReserveStatus(record, ReserveStatus.CANCEL,
                release ? resource : null, "AVAILABLE", null);
        return Result.success("预约取消成功。", record);
    }

    public Result<List<ReserveRecord>> queryAllReserves(User user) {
        ensureAdmin(user);
        return collectReserveQuery(records.findAll().stream(),
                "查询全部预约成功。", "当前没有预约记录。");
    }

    public Result<List<ReserveRecord>> queryPendingReserves(User user) {
        requireAdminReserveAccess(user);
        return collectReserveQuery(records.findAll().stream()
                .filter(record -> record.getStatus() == ReserveStatus.PENDING),
                "查询待审核预约成功。", "当前没有待审核预约。");
    }

    public Result<ReserveRecord> auditReserve(User user, String recordId, String decision,
            String rejectReason) {
        ensureAdmin(user);
        ReserveRecord record = Optional.ofNullable(records.findById(recordId))
                .orElseThrow(() -> new BusinessException("预约记录不存在，无法审核。"));
        ReserveStatus nextStatus = "2".equals(decision) ? ReserveStatus.REJECT : ReserveStatus.PASS;
        requireTransition(record.getStatus(), nextStatus);
        if (!"1".equals(decision) && !"2".equals(decision)) {
            throw new BusinessException("审核选项无效，请输入 1 或 2。");
        }
        if ("2".equals(decision)) {
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                throw new ParameterEmptyException("驳回原因不能为空。");
            }
            updateReserveStatus(record, ReserveStatus.REJECT, null, null, rejectReason);
            return Result.success("预约审核驳回。", record);
        }

        CampusResource resource = resources.findById(record.getResourceId());
        if (resource == null) {
            throw new ResourceNotFoundException("关联资源不存在，无法审核通过。");
        }
        if (hasPassConflict(record.getResourceId(),
                record.getReserveStartTime(), record.getReserveEndTime())) {
            throw new ResourceOccupiedException("预约时间冲突：该资源已有已通过的预约。");
        }
        boolean otherPass = hasPassRecord(record.getResourceId());
        if (!"AVAILABLE".equals(resource.getStatus())
                && !(otherPass && "OCCUPIED".equals(resource.getStatus()))) {
            throw new ResourceOccupiedException("资源当前不可审核通过。");
        }
        updateReserveStatus(record, ReserveStatus.PASS, resource, "OCCUPIED", "");
        return Result.success("预约审核通过。", record);
    }

    public Result<List<ReserveRecord>> queryReserves(User user, String field, String value) {
        ensureAdmin(user);
        if (value == null || value.isEmpty()) {
            throw new ParameterEmptyException("预约查询值不能为空。");
        }
        Stream<ReserveRecord> matched = records.findAll().stream();
        if ("1".equals(field)) {
            matched = matched.filter(record -> value.equals(record.getResourceId()));
        } else if ("2".equals(field)) {
            matched = matched.filter(record -> value.equals(record.getApplicantUsername()));
        } else if ("3".equals(field)) {
            ReserveStatus status;
            try {
                status = ReserveStatus.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("预约状态无效，请输入 PENDING、PASS、REJECT 或 CANCEL。");
            }
            matched = matched.filter(record -> status == record.getStatus());
        } else {
            throw new BusinessException("查询条件无效，请输入 1、2 或 3。");
        }
        return collectReserveQuery(matched, "预约查询成功。", "未找到匹配的预约记录。");
    }

    private Result<List<ReserveRecord>> collectReserveQuery(
            Stream<ReserveRecord> stream, String successMsg, String emptyMsg) {
        List<ReserveRecord> matched = stream.collect(Collectors.toList());
        return Result.success(matched.isEmpty() ? emptyMsg : successMsg, matched);
    }

    private boolean hasPassRecord(String resourceId) {
        return records.findAll().stream().anyMatch(record -> resourceId.equals(record.getResourceId())
                && record.getStatus() == ReserveStatus.PASS);
    }

    private boolean hasPassConflict(String resourceId, LocalDateTime start, LocalDateTime end) {
        return records.findAll().stream().anyMatch(record -> resourceId.equals(record.getResourceId())
                && record.getStatus() == ReserveStatus.PASS
                && start.isBefore(record.getReserveEndTime())
                && record.getReserveStartTime().isBefore(end));
    }

    private String generateReserveId() {
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (records.findById(id) != null);
        return id;
    }

    private void requireTransition(ReserveStatus current, ReserveStatus next) {
        boolean allowed = current == ReserveStatus.PENDING
                && (next == ReserveStatus.PASS || next == ReserveStatus.REJECT
                        || next == ReserveStatus.CANCEL)
                || current == ReserveStatus.PASS && next == ReserveStatus.CANCEL;
        if (!allowed) {
            if (next == ReserveStatus.CANCEL) {
                throw new InvalidReserveStatusException("当前预约状态不可取消。");
            }
            if (next == ReserveStatus.PASS || next == ReserveStatus.REJECT) {
                throw new InvalidReserveStatusException("该预约已处理，不能重复审核。");
            }
            throw new InvalidReserveStatusException("预约状态变更不合法。");
        }
    }

    private void updateReserveStatus(ReserveRecord record, ReserveStatus nextStatus,
            CampusResource resource, String nextResourceStatus, String rejectReason) {
        ReserveStatus oldStatus = record.getStatus();
        String oldRejectReason = record.getRejectReason();
        LocalDateTime oldAuditTime = record.getAuditTime();
        String oldResourceStatus = resource == null ? null : resource.getStatus();
        record.setStatus(nextStatus);
        // 取消仅改变状态，保留已有审核信息；审核失败时三个字段一起恢复。
        if (nextStatus == ReserveStatus.PASS || nextStatus == ReserveStatus.REJECT) {
            record.setRejectReason(rejectReason);
            record.setAuditTime(LocalDateTime.now());
        }
        if (resource != null) {
            resource.setStatus(nextResourceStatus);
        }
        boolean reserveSaved = false;
        try {
            records.save();
            reserveSaved = true;
            if (resource != null) {
                resources.save();
            }
        } catch (IOException e) {
            record.setStatus(oldStatus);
            record.setRejectReason(oldRejectReason);
            record.setAuditTime(oldAuditTime);
            if (resource != null) {
                resource.setStatus(oldResourceStatus);
            }
            if (reserveSaved) {
                try {
                    records.save();
                } catch (IOException rollbackError) {
                    throw new BusinessException(
                            "数据保存失败且预约状态恢复失败，请检查 reserve.txt 和 resource.txt。");
                }
            }
            throw new BusinessException(reserveSaved
                    ? "资源数据保存失败，请检查 resource.txt。"
                    : "预约数据保存失败，请检查 reserve.txt。");
        }
    }

    private void saveReserves() {
        try {
            records.save();
        } catch (IOException e) {
            throw new BusinessException("预约数据保存失败，请检查 reserve.txt。");
        }
    }

    private void ensureStudent(User user) {
        if (!"student".equals(user.getRole())) {
            throw new PermissionDeniedException("权限不足，仅学生可以办理个人预约。");
        }
    }

    private void ensureAdmin(User user) {
        if (!"admin".equals(user.getRole())) {
            throw new PermissionDeniedException("权限不足，仅管理员可以管理校园资源。");
        }
    }
}
