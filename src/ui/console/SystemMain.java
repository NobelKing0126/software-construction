package ui.console;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

import dao.FileReserveRecordDao;
import dao.FileResourceDao;
import dao.FileUserDao;
import dao.ReserveRecordDao;
import dao.ResourceDao;
import dao.UserDao;
import entity.CampusResource;
import entity.ReserveRecord;
import entity.User;
import exception.BusinessException;
import service.ReserveRecordService;
import service.ResourceService;
import service.UserService;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public class SystemMain {
    private final UserService userService;
    private final ResourceService resourceService;
    private final ReserveRecordService reserveService;

    private SystemMain(UserService userService, ResourceService resourceService,
            ReserveRecordService reserveService) {
        this.userService = userService;
        this.resourceService = resourceService;
        this.reserveService = reserveService;
    }

    public static void main(String[] args) {
        UserDao users;
        try {
            users = new FileUserDao();
        } catch (Exception e) {
            showResult(Result.fail("用户数据加载失败，请检查 user.txt。"));
            return;
        }
        ResourceDao resources;
        try {
            resources = new FileResourceDao();
        } catch (Exception e) {
            showResult(Result.fail("资源数据加载失败，请检查 resource.txt。"));
            return;
        }
        ReserveRecordDao records;
        try {
            records = new FileReserveRecordDao();
        } catch (Exception e) {
            showResult(Result.fail("预约数据加载失败，请检查 reserve.txt。"));
            return;
        }

        SystemMain app = new SystemMain(new UserService(users),
                new ResourceService(resources, records),
                new ReserveRecordService(records, resources));
        app.run();
    }

    private void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("校园资源预约系统");
            System.out.println("1. 登录");
            System.out.println("2. 注册");
            System.out.println("0. 退出");
            System.out.print("请选择：");
            String choice = scanner.nextLine();

            try {
                if ("1".equals(choice)) {
                    System.out.print("请输入账号：");
                    String username = scanner.nextLine().trim();
                    System.out.print("请输入密码：");
                    String password = scanner.nextLine();
                    Result<User> result = userService.login(username, password);
                    System.out.println(result.getMsg());
                    if (result.isSuccess()) {
                        resourceMenu(scanner, result.getData());
                    }
                } else if ("2".equals(choice)) {
                    System.out.print("请输入注册账号：");
                    String username = scanner.nextLine().trim();
                    System.out.print("请输入注册密码：");
                    String password = scanner.nextLine();
                    showResult(userService.register(username, password));
                } else if ("0".equals(choice)) {
                    System.out.println("已退出系统。");
                    break;
                } else {
                    System.out.println("无效选项，请重新选择。");
                }
            } catch (BusinessException e) {
                showResult(Result.fail(e.getMessage()));
            } catch (IOException e) {
                showResult(Result.fail("用户数据保存失败，请检查 user.txt。"));
            } catch (Exception e) {
                showResult(Result.fail("系统操作失败，请稍后重试。"));
            }
        }
        scanner.close();
    }

    private void resourceMenu(Scanner scanner, User user) {
        userService.requireResourceAccess(user);

        while (true) {
            boolean admin = "admin".equals(user.getRole());
            if (admin) {
                System.out.println("管理员资源管理菜单");
                System.out.println("1. 新增资源");
                System.out.println("2. 查询全部资源");
                System.out.println("3. 修改资源");
                System.out.println("4. 删除资源");
                System.out.println("5. 按名称查询资源");
                System.out.println("6. 按 ID 查询资源");
                System.out.println("7. 查询全部预约");
                System.out.println("8. 按记录 ID 审核预约");
                System.out.println("9. 按条件查询预约");
                System.out.println("0. 退出登录");
            } else {
                System.out.println("学生资源查询菜单");
                System.out.println("1. 查询可预约资源");
                System.out.println("5. 按名称查询资源");
                System.out.println("6. 按 ID 查询资源");
                System.out.println("10. 提交预约申请");
                System.out.println("11. 查询我的预约");
                System.out.println("12. 取消我的预约");
                System.out.println("0. 退出登录");
            }
            System.out.print("请选择：");
            String choice = scanner.nextLine();

            try {
                if ("0".equals(choice)) {
                    System.out.println("已退出登录。");
                    return;
                }
                if ("5".equals(choice)) {
                    System.out.print("请输入资源名称关键词：");
                    showResult(resourceService.queryResourcesByName(user, scanner.nextLine().trim()));
                } else if ("6".equals(choice)) {
                    System.out.print("请输入资源 ID：");
                    showResult(resourceService.queryResourceById(user, scanner.nextLine().trim()));
                } else if (admin) {
                    if ("1".equals(choice)) {
                        showResult(readAndAddResource(scanner, user));
                    } else if ("2".equals(choice)) {
                        showResult(resourceService.queryAllResources(user));
                    } else if ("3".equals(choice)) {
                        showResult(readAndModifyResource(scanner, user));
                    } else if ("4".equals(choice)) {
                        showResult(readAndDeleteResource(scanner, user));
                    } else if ("7".equals(choice)) {
                        showResult(reserveService.queryAllReserves(user));
                    } else if ("8".equals(choice)) {
                        showResult(readAndAuditReserve(scanner, user));
                    } else if ("9".equals(choice)) {
                        showResult(readAndQueryReserves(scanner, user));
                    } else if ("10".equals(choice) || "11".equals(choice)
                            || "12".equals(choice)) {
                        reserveService.requireStudentAccess(user);
                    } else {
                        System.out.println("无效选项，请重新选择。");
                    }
                } else if ("1".equals(choice)) {
                    showResult(resourceService.queryAvailableResources(user));
                } else if ("2".equals(choice)) {
                    showResult(resourceService.addResource(user, null, null, null, null));
                } else if ("3".equals(choice)) {
                    showResult(resourceService.modifyResource(user, null, null, null, null, null));
                } else if ("4".equals(choice)) {
                    showResult(resourceService.deleteResource(user, null));
                } else if ("10".equals(choice)) {
                    showResult(readAndSubmitReserve(scanner, user));
                } else if ("11".equals(choice)) {
                    showResult(reserveService.queryMyReserves(user));
                } else if ("12".equals(choice)) {
                    showResult(readAndCancelReserve(scanner, user));
                } else if ("7".equals(choice) || "8".equals(choice)
                        || "9".equals(choice)) {
                    reserveService.requireAdminReserveAccess(user);
                } else {
                    System.out.println("无效选项，请重新选择。");
                }
            } catch (BusinessException e) {
                showResult(Result.fail(e.getMessage()));
            } catch (IOException e) {
                showResult(Result.fail("资源数据保存失败，请检查 resource.txt。"));
            } catch (Exception e) {
                showResult(Result.fail("系统操作失败，请稍后重试。"));
            }
        }
    }

    private Result<CampusResource> readAndAddResource(Scanner scanner, User user)
            throws IOException {
        System.out.print("请输入资源名称：");
        String name = scanner.nextLine().trim();
        System.out.print("请输入资源类型：");
        String type = scanner.nextLine().trim();
        System.out.print("请输入资源位置：");
        String location = scanner.nextLine().trim();
        System.out.print("请输入资源描述：");
        String description = scanner.nextLine().trim();
        return resourceService.addResource(user, name, type, location, description);
    }

    private Result<CampusResource> readAndModifyResource(Scanner scanner, User user)
            throws IOException {
        System.out.print("请输入要修改的资源 ID：");
        String id = scanner.nextLine().trim();
        resourceService.requireResourceForModification(user, id);
        System.out.print("请输入新的资源名称：");
        String name = scanner.nextLine().trim();
        System.out.print("请输入新的资源类型：");
        String type = scanner.nextLine().trim();
        System.out.print("请输入新的资源位置：");
        String location = scanner.nextLine().trim();
        System.out.print("请输入新的资源状态：");
        String status = scanner.nextLine().trim();
        return resourceService.modifyResource(user, id, name, type, location, status);
    }

    private Result<Void> readAndDeleteResource(Scanner scanner, User user) throws IOException {
        System.out.print("请输入要删除的资源 ID：");
        return resourceService.deleteResource(user, scanner.nextLine().trim());
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
        return reserveService.auditReserve(user, recordId, scanner.nextLine().trim());
    }

    private Result<List<ReserveRecord>> readAndQueryReserves(Scanner scanner, User user) {
        System.out.print("查询条件（1. 资源 ID  2. 申请人  3. 预约状态）：");
        String field = scanner.nextLine().trim();
        System.out.print("请输入查询值：");
        return reserveService.queryReserves(user, field, scanner.nextLine().trim());
    }

    private static void showResult(Result<?> result) {
        System.out.println(result.getMsg());
        if (result.isSuccess() && result.getData() != null) {
            if (result.getData() instanceof List<?>) {
                for (Object item : (List<?>) result.getData()) {
                    System.out.println(item);
                }
            } else {
                System.out.println(result.getData());
            }
        }
    }
}
