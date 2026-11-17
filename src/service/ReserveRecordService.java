package service;

import java.time.LocalDateTime;
import java.util.List;

import entity.ReserveRecord;
import entity.User;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public interface ReserveRecordService {
    void requireStudentAccess(User user);

    void requireAdminReserveAccess(User user);

    LocalDateTime parseReserveTime(String value);

    Result<ReserveRecord> submitReserve(User user, String resourceId,
            LocalDateTime start, LocalDateTime end, String remark);

    Result<List<ReserveRecord>> queryMyReserves(User user);

    Result<ReserveRecord> cancelReserve(User user, String recordId);

    Result<List<ReserveRecord>> queryAllReserves(User user);

    Result<List<ReserveRecord>> queryPendingReserves(User user);

    Result<ReserveRecord> auditReserve(User user, String recordId, String decision,
            String rejectReason);

    Result<List<ReserveRecord>> queryReserves(User user, String field, String value);
}
