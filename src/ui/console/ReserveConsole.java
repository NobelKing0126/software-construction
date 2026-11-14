package ui.console;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

import entity.ReserveRecord;
import entity.User;
import service.ReserveRecordService;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
final class ReserveConsole {
    private final ReserveRecordService reserveService;

    ReserveConsole(ReserveRecordService reserveService) {
        this.reserveService = reserveService;
    }

    // 返回是否识别该选项；未识别时不读取输入或调用 Service。
    boolean handleChoice(Scanner scanner, User user, String choice) {
        Result<?> result;
        switch (choice) {
            case "7":
                reserveService.requireAdminReserveAccess(user);
                result = reserveService.queryAllReserves(user);
                break;
            case "8":
                reserveService.requireAdminReserveAccess(user);
                result = readAndAuditReserve(scanner, user);
                break;
            case "9":
                reserveService.requireAdminReserveAccess(user);
                result = readAndQueryReserves(scanner, user);
                break;
            case "13":
                reserveService.requireAdminReserveAccess(user);
                result = reserveService.queryPendingReserves(user);
                break;
            case "10":
                reserveService.requireStudentAccess(user);
                result = readAndSubmitReserve(scanner, user);
                break;
            case "11":
                reserveService.requireStudentAccess(user);
                result = reserveService.queryMyReserves(user);
                break;
            case "12":
                reserveService.requireStudentAccess(user);
                result = readAndCancelReserve(scanner, user);
                break;
            default:
                return false;
        }
        ConsoleOutput.showResult(result);
        return true;
    }

    private Result<ReserveRecord> readAndSubmitReserve(Scanner scanner, User user) {
        System.out.print("请输入可预约资源 ID：");
        String resourceId = scanner.nextLine().trim();
        System.out.print("请输入开始时间（yyyy-MM-dd HH:mm）：");
        LocalDateTime start = reserveService.parseReserveTime(scanner.nextLine());
        System.out.print("请输入结束时间（yyyy-MM-dd HH:mm）：");
        LocalDateTime end = reserveService.parseReserveTime(scanner.nextLine());
        System.out.print("请输入备注（可留空）：");
        String remark = scanner.nextLine();
        return reserveService.submitReserve(user, resourceId, start, end, remark);
    }

    private Result<ReserveRecord> readAndCancelReserve(Scanner scanner, User user) {
        System.out.print("请输入要取消的预约记录 ID：");
        return reserveService.cancelReserve(user, scanner.nextLine().trim());
    }

    private Result<ReserveRecord> readAndAuditReserve(Scanner scanner, User user) {
        System.out.print("请输入要审核的预约记录 ID：");
        String recordId = scanner.nextLine().trim();
        System.out.print("请选择审核结果（1. 通过  2. 驳回）：");
        String decision = scanner.nextLine().trim();
        String rejectReason = "";
        if ("2".equals(decision)) {
            System.out.print("请输入驳回原因：");
            rejectReason = scanner.nextLine();
        }
        return reserveService.auditReserve(user, recordId, decision, rejectReason);
    }

    private Result<List<ReserveRecord>> readAndQueryReserves(Scanner scanner, User user) {
        System.out.print("查询条件（1. 资源 ID  2. 申请人  3. 预约状态）：");
        String field = scanner.nextLine().trim();
        System.out.print("请输入查询值：");
        return reserveService.queryReserves(user, field, scanner.nextLine().trim());
    }
}
