import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public class SystemMain {
    private static final DateTimeFormatter RESERVE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        List<User> users;
        try {
            users = FileUtil.loadUsers();
        } catch (Exception e) {
            showResult(Result.fail("用户数据加载失败，请检查 user.txt。"));
            return;
        }
        List<CampusResource> resources;
        try {
            resources = FileUtil.loadResources();
        } catch (Exception e) {
            showResult(Result.fail("资源数据加载失败，请检查 resource.txt。"));
            return;
        }
        List<ReserveRecord> records;
        try {
            records = FileUtil.loadReserves();
        } catch (Exception e) {
            showResult(Result.fail("预约数据加载失败，请检查 reserve.txt。"));
            return;
        }
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
                    Result<User> result = login(users, username, password);
                    System.out.println(result.getMsg());
                    if (result.isSuccess()) {
                        resourceMenu(scanner, result.getData(), resources, records);
                    }
                } else if ("2".equals(choice)) {
                    System.out.print("请输入注册账号：");
                    String username = scanner.nextLine().trim();
                    System.out.print("请输入注册密码：");
                    String password = scanner.nextLine();
                    showResult(register(users, username, password));
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

    private static Result<User> login(List<User> users, String username, String password) {
        if (!isUsernameValid(username)) {
            throw new ParameterEmptyException("账号不能为空。");
        }
        if (password == null || password.isEmpty()) {
            throw new ParameterEmptyException("密码不能为空。");
        }
        Result<User> found = findUserByUsername(users, username);
        if (!found.isSuccess()) {
            throw new UserNotFoundException("用户不存在，登录失败。");
        }
        User user = found.getData();
        if (!password.equals(user.getPassword())) {
            throw new PasswordErrorException("密码错误，登录失败。");
        }
        return Result.success("登录成功，欢迎使用校园资源预约系统！", user);
    }

    private static void resourceMenu(Scanner scanner, User user,
            List<CampusResource> resources, List<ReserveRecord> records) {
        if (!"admin".equals(user.getRole()) && !"student".equals(user.getRole())) {
            throw new PermissionDeniedException("当前账号角色无资源操作权限。");
        }

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
                    showResult(queryResourcesByName(user, resources, scanner.nextLine().trim()));
                } else if ("6".equals(choice)) {
                    System.out.print("请输入资源 ID：");
                    showResult(queryResourceById(user, resources, scanner.nextLine().trim()));
                } else if (admin) {
                    if ("1".equals(choice)) {
                        showResult(readAndAddResource(scanner, user, resources));
                    } else if ("2".equals(choice)) {
                        showResult(queryAllResources(user, resources));
                    } else if ("3".equals(choice)) {
                        showResult(readAndModifyResource(scanner, user, resources, records));
                    } else if ("4".equals(choice)) {
                        showResult(readAndDeleteResource(scanner, user, resources, records));
                    } else if ("7".equals(choice)) {
                        showResult(queryAllReserves(user, records));
                    } else if ("8".equals(choice)) {
                        showResult(readAndAuditReserve(scanner, user, resources, records));
                    } else if ("9".equals(choice)) {
                        showResult(readAndQueryReserves(scanner, user, records));
                    } else if ("10".equals(choice) || "11".equals(choice)
                            || "12".equals(choice)) {
                        throw new PermissionDeniedException("权限不足，仅学生可以办理个人预约。");
                    } else {
                        System.out.println("无效选项，请重新选择。");
                    }
                } else if ("1".equals(choice)) {
                    showResult(queryAvailableResources(user, resources));
                } else if ("2".equals(choice)) {
                    showResult(addResource(user, resources, null, null, null, null));
                } else if ("3".equals(choice)) {
                    showResult(modifyResource(user, resources, records,
                            null, null, null, null, null));
                } else if ("4".equals(choice)) {
                    showResult(deleteResource(user, resources, records, null));
                } else if ("10".equals(choice)) {
                    showResult(readAndSubmitReserve(scanner, user, resources, records));
                } else if ("11".equals(choice)) {
                    showResult(queryMyReserves(user, records));
                } else if ("12".equals(choice)) {
                    showResult(readAndCancelReserve(scanner, user, resources, records));
                } else if ("7".equals(choice) || "8".equals(choice)
                        || "9".equals(choice)) {
                    throw new PermissionDeniedException("权限不足，仅管理员可以审核或查询全部预约。");
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

    private static Result<CampusResource> readAndAddResource(Scanner scanner, User user,
            List<CampusResource> resources) throws IOException {
        System.out.print("请输入资源名称：");
        String name = scanner.nextLine().trim();
        System.out.print("请输入资源类型：");
        String type = scanner.nextLine().trim();
        System.out.print("请输入资源位置：");
        String location = scanner.nextLine().trim();
        System.out.print("请输入资源描述：");
        String description = scanner.nextLine().trim();
        return addResource(user, resources, name, type, location, description);
    }

    private static Result<CampusResource> addResource(User user, List<CampusResource> resources,
            String name, String type, String location, String description) throws IOException {
        ensureAdmin(user);
        if (name == null || name.isEmpty()) {
            throw new ParameterEmptyException("资源名称不能为空。");
        }

        String id = generateResourceId(resources);
        CampusResource resource = new CampusResource(id, name, type, "AVAILABLE", location,
                description);
        resources.add(resource);
        FileUtil.saveResources(resources);
        return Result.success("资源新增成功，资源 ID：" + id, resource);
    }

    private static Result<List<CampusResource>> queryAllResources(User user,
            List<CampusResource> resources) {
        ensureAdmin(user);
        return collectResourceQuery(resources.stream(),
                "查询全部资源成功。", "当前没有校园资源。");
    }

    private static Result<List<CampusResource>> queryAvailableResources(User user,
            List<CampusResource> resources) {
        if (!"student".equals(user.getRole())) {
            throw new PermissionDeniedException("只有学生可以查询可预约资源。");
        }
        return collectResourceQuery(resources.stream()
                .filter(resource -> "AVAILABLE".equals(resource.getStatus())),
                "查询可预约资源成功。", "当前没有可预约资源。");
    }

    private static Result<List<CampusResource>> queryResourcesByName(User user,
            List<CampusResource> resources, String keyword) {
        boolean admin = "admin".equals(user.getRole());
        if (!admin && !"student".equals(user.getRole())) {
            throw new PermissionDeniedException("当前账号角色无资源操作权限。");
        }
        if (keyword == null || keyword.isEmpty()) {
            throw new ParameterEmptyException("资源名称关键词不能为空。");
        }
        return collectResourceQuery(resources.stream()
                .filter(resource -> admin || "AVAILABLE".equals(resource.getStatus()))
                .filter(resource -> resource.getName().contains(keyword)),
                "按名称查询资源成功。", "未找到匹配的资源。");
    }

    private static Result<List<CampusResource>> collectResourceQuery(
            Stream<CampusResource> stream, String successMsg, String emptyMsg) {
        List<CampusResource> matched = stream
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
        return Result.success(matched.isEmpty() ? emptyMsg : successMsg, matched);
    }

    private static Result<CampusResource> queryResourceById(User user,
            List<CampusResource> resources, String id) {
        if (!"admin".equals(user.getRole()) && !"student".equals(user.getRole())) {
            throw new PermissionDeniedException("当前账号角色无资源操作权限。");
        }
        Result<CampusResource> found = findResourceById(resources, id);
        if ("student".equals(user.getRole()) && found.isSuccess()
                && !"AVAILABLE".equals(found.getData().getStatus())) {
            return Result.fail("未找到该 ID 对应的资源。");
        }
        return found;
    }

    private static Result<CampusResource> readAndModifyResource(Scanner scanner, User user,
            List<CampusResource> resources, List<ReserveRecord> records) throws IOException {
        System.out.print("请输入要修改的资源 ID：");
        String id = scanner.nextLine().trim();
        Result<CampusResource> found = findResourceById(resources, id);
        if (!found.isSuccess()) {
            throw new ResourceNotFoundException("资源不存在，无法修改。");
        }
        System.out.print("请输入新的资源名称：");
        String name = scanner.nextLine().trim();
        System.out.print("请输入新的资源类型：");
        String type = scanner.nextLine().trim();
        System.out.print("请输入新的资源位置：");
        String location = scanner.nextLine().trim();
        System.out.print("请输入新的资源状态：");
        String status = scanner.nextLine().trim();
        return modifyResource(user, resources, records, id, name, type, location, status);
    }

    private static Result<CampusResource> modifyResource(User user,
            List<CampusResource> resources, List<ReserveRecord> records,
            String id, String name, String type, String location, String status)
            throws IOException {
        ensureAdmin(user);
        Result<CampusResource> found = findResourceById(resources, id);
        if (!found.isSuccess()) {
            throw new ResourceNotFoundException("资源不存在，无法修改。");
        }
        if (name == null || name.isEmpty()) {
            throw new ParameterEmptyException("资源名称不能为空。");
        }
        if (!isResourceStatusValid(status)) {
            throw new InvalidResourceStatusException(
                    "资源状态不合法，只能是 AVAILABLE、OCCUPIED 或 DISABLED。");
        }
        if (!"OCCUPIED".equals(status) && records.stream()
                .anyMatch(record -> id.equals(record.getResourceId())
                        && record.getStatus() == ReserveStatus.PASS)) {
            throw new BusinessException("资源存在已通过预约，状态必须保持 OCCUPIED。");
        }

        CampusResource resource = found.getData();
        resource.setName(name);
        resource.setType(type);
        resource.setLocation(location);
        resource.setStatus(status);
        FileUtil.saveResources(resources);
        return Result.success("资源修改成功。", resource);
    }

    private static Result<Void> readAndDeleteResource(Scanner scanner, User user,
            List<CampusResource> resources, List<ReserveRecord> records) throws IOException {
        System.out.print("请输入要删除的资源 ID：");
        return deleteResource(user, resources, records, scanner.nextLine().trim());
    }

    private static Result<Void> deleteResource(User user,
            List<CampusResource> resources, List<ReserveRecord> records, String id)
            throws IOException {
        ensureAdmin(user);
        Result<CampusResource> found = findResourceById(resources, id);
        if (!found.isSuccess()) {
            throw new ResourceNotFoundException("资源不存在，无法删除。");
        }
        if (records.stream().anyMatch(record -> id.equals(record.getResourceId())
                && record.getStatus() == ReserveStatus.PASS)) {
            throw new BusinessException("存在已通过预约，不能删除资源。");
        }
        resources.remove(found.getData());
        FileUtil.saveResources(resources);
        return Result.success("资源删除成功。");
    }

    private static Result<ReserveRecord> readAndSubmitReserve(Scanner scanner, User user,
            List<CampusResource> resources, List<ReserveRecord> records) {
        System.out.print("请输入可预约资源 ID：");
        String resourceId = scanner.nextLine().trim();
        System.out.print("请输入开始时间（yyyy-MM-dd HH:mm）：");
        LocalDateTime start = parseReserveTime(scanner.nextLine().trim());
        System.out.print("请输入结束时间（yyyy-MM-dd HH:mm）：");
        LocalDateTime end = parseReserveTime(scanner.nextLine().trim());
        System.out.print("请输入备注（可留空）：");
        String remark = scanner.nextLine();
        return submitReserve(user, resources, records, resourceId, start, end, remark);
    }

    private static Result<ReserveRecord> submitReserve(User user,
            List<CampusResource> resources, List<ReserveRecord> records,
            String resourceId, LocalDateTime start, LocalDateTime end, String remark) {
        ensureStudent(user);
        Result<CampusResource> found = findResourceById(resources, resourceId);
        if (!found.isSuccess()) {
            throw new ResourceNotFoundException("资源不存在，无法提交预约。");
        }
        if (!start.isBefore(end)) {
            throw new BusinessException("开始时间必须早于结束时间。");
        }
        if (end.isBefore(LocalDateTime.now())) {
            throw new BusinessException("结束时间不能早于当前时间。");
        }
        if (hasPassConflict(records, resourceId, start, end)) {
            throw new BusinessException("预约时间冲突：该资源已有已通过的预约。");
        }
        if (!"AVAILABLE".equals(found.getData().getStatus())) {
            throw new BusinessException("资源当前不可预约。");
        }

        String recordId = generateReserveId(records);
        ReserveRecord record = new ReserveRecord(recordId, resourceId, user.getUsername(),
                LocalDateTime.now(), start, end, ReserveStatus.PENDING, remark);
        records.add(record);
        try {
            saveReserves(records);
        } catch (BusinessException e) {
            records.remove(record);
            throw e;
        }
        return Result.success("预约申请提交成功，记录 ID：" + recordId, record);
    }

    private static Result<List<ReserveRecord>> queryMyReserves(User user,
            List<ReserveRecord> records) {
        ensureStudent(user);
        return collectReserveQuery(records.stream()
                .filter(record -> user.getUsername().equals(record.getApplicantUsername())),
                "查询我的预约成功。", "当前没有预约记录。");
    }

    private static Result<ReserveRecord> readAndCancelReserve(Scanner scanner, User user,
            List<CampusResource> resources, List<ReserveRecord> records) {
        System.out.print("请输入要取消的预约记录 ID：");
        return cancelReserve(user, resources, records, scanner.nextLine().trim());
    }

    private static Result<ReserveRecord> cancelReserve(User user,
            List<CampusResource> resources, List<ReserveRecord> records, String recordId) {
        ensureStudent(user);
        Result<ReserveRecord> found = findReserveById(records, recordId);
        if (!found.isSuccess()) {
            throw new BusinessException("预约记录不存在，无法取消。");
        }
        ReserveRecord record = found.getData();
        if (!user.getUsername().equals(record.getApplicantUsername())) {
            throw new PermissionDeniedException("只能取消本人的预约。");
        }
        if (record.getStatus() != ReserveStatus.PENDING
                && record.getStatus() != ReserveStatus.PASS) {
            throw new BusinessException("当前预约状态不可取消。");
        }

        boolean wasPass = record.getStatus() == ReserveStatus.PASS;
        CampusResource resource = null;
        if (wasPass) {
            Result<CampusResource> resourceResult =
                    findResourceById(resources, record.getResourceId());
            if (!resourceResult.isSuccess()) {
                throw new ResourceNotFoundException("关联资源不存在，无法取消已通过预约。");
            }
            resource = resourceResult.getData();
        }
        boolean release = wasPass && records.stream().noneMatch(other -> other != record
                && record.getResourceId().equals(other.getResourceId())
                && other.getStatus() == ReserveStatus.PASS);
        updateReserveStatus(records, resources, record, ReserveStatus.CANCEL,
                release ? resource : null, "AVAILABLE");
        return Result.success("预约取消成功。", record);
    }

    private static Result<List<ReserveRecord>> queryAllReserves(User user,
            List<ReserveRecord> records) {
        ensureAdmin(user);
        return collectReserveQuery(records.stream(),
                "查询全部预约成功。", "当前没有预约记录。");
    }

    private static Result<ReserveRecord> readAndAuditReserve(Scanner scanner, User user,
            List<CampusResource> resources, List<ReserveRecord> records) {
        System.out.print("请输入要审核的预约记录 ID：");
        String recordId = scanner.nextLine().trim();
        System.out.print("请选择审核结果（1. 通过  2. 驳回）：");
        return auditReserve(user, resources, records, recordId, scanner.nextLine().trim());
    }

    private static Result<ReserveRecord> auditReserve(User user,
            List<CampusResource> resources, List<ReserveRecord> records,
            String recordId, String decision) {
        ensureAdmin(user);
        Result<ReserveRecord> found = findReserveById(records, recordId);
        if (!found.isSuccess()) {
            throw new BusinessException("预约记录不存在，无法审核。");
        }
        ReserveRecord record = found.getData();
        if (record.getStatus() != ReserveStatus.PENDING) {
            throw new BusinessException("该预约已处理，不能重复审核。");
        }
        if (!"1".equals(decision) && !"2".equals(decision)) {
            throw new BusinessException("审核选项无效，请输入 1 或 2。");
        }
        if ("2".equals(decision)) {
            updateReserveStatus(records, resources, record, ReserveStatus.REJECT,
                    null, null);
            return Result.success("预约审核驳回。", record);
        }

        Result<CampusResource> resourceResult =
                findResourceById(resources, record.getResourceId());
        if (!resourceResult.isSuccess()) {
            throw new ResourceNotFoundException("关联资源不存在，无法审核通过。");
        }
        CampusResource resource = resourceResult.getData();
        if (hasPassConflict(records, record.getResourceId(),
                record.getReserveStartTime(), record.getReserveEndTime())) {
            throw new BusinessException("预约时间冲突：该资源已有已通过的预约。");
        }
        boolean otherPass = hasPassRecord(records, record.getResourceId());
        if (!"AVAILABLE".equals(resource.getStatus())
                && !(otherPass && "OCCUPIED".equals(resource.getStatus()))) {
            throw new BusinessException("资源当前不可审核通过。");
        }
        updateReserveStatus(records, resources, record, ReserveStatus.PASS,
                resource, "OCCUPIED");
        return Result.success("预约审核通过。", record);
    }

    private static Result<List<ReserveRecord>> readAndQueryReserves(Scanner scanner,
            User user, List<ReserveRecord> records) {
        System.out.print("查询条件（1. 资源 ID  2. 申请人  3. 预约状态）：");
        String field = scanner.nextLine().trim();
        System.out.print("请输入查询值：");
        return queryReserves(user, records, field, scanner.nextLine().trim());
    }

    private static Result<List<ReserveRecord>> queryReserves(User user,
            List<ReserveRecord> records, String field, String value) {
        ensureAdmin(user);
        if (value == null || value.isEmpty()) {
            throw new ParameterEmptyException("预约查询值不能为空。");
        }
        Stream<ReserveRecord> matched = records.stream();
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

    private static Result<List<ReserveRecord>> collectReserveQuery(
            Stream<ReserveRecord> stream, String successMsg, String emptyMsg) {
        List<ReserveRecord> matched = stream.collect(Collectors.toList());
        return Result.success(matched.isEmpty() ? emptyMsg : successMsg, matched);
    }

    private static Result<ReserveRecord> findReserveById(
            List<ReserveRecord> records, String recordId) {
        Optional<ReserveRecord> found = records.stream()
                .filter(record -> recordId != null && recordId.equals(record.getRecordId()))
                .findFirst();
        if (found.isPresent()) {
            return Result.success("查询预约成功。", found.get());
        }
        return Result.fail("未找到该 ID 对应的预约记录。");
    }

    private static boolean hasPassRecord(List<ReserveRecord> records, String resourceId) {
        return records.stream().anyMatch(record -> resourceId.equals(record.getResourceId())
                && record.getStatus() == ReserveStatus.PASS);
    }

    private static boolean hasPassConflict(List<ReserveRecord> records, String resourceId,
            LocalDateTime start, LocalDateTime end) {
        return records.stream().anyMatch(record -> resourceId.equals(record.getResourceId())
                && record.getStatus() == ReserveStatus.PASS
                && start.isBefore(record.getReserveEndTime())
                && record.getReserveStartTime().isBefore(end));
    }

    private static LocalDateTime parseReserveTime(String value) {
        try {
            return LocalDateTime.parse(value, RESERVE_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new BusinessException("时间格式错误，请使用 yyyy-MM-dd HH:mm。");
        }
    }

    private static String generateReserveId(List<ReserveRecord> records) {
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (findReserveById(records, id).isSuccess());
        return id;
    }

    private static void updateReserveStatus(List<ReserveRecord> records,
            List<CampusResource> resources, ReserveRecord record, ReserveStatus nextStatus,
            CampusResource resource, String nextResourceStatus) {
        ReserveStatus oldStatus = record.getStatus();
        String oldResourceStatus = resource == null ? null : resource.getStatus();
        record.setStatus(nextStatus);
        if (resource != null) {
            resource.setStatus(nextResourceStatus);
        }
        boolean reserveSaved = false;
        try {
            FileUtil.saveReserves(records);
            reserveSaved = true;
            if (resource != null) {
                FileUtil.saveResources(resources);
            }
        } catch (IOException e) {
            record.setStatus(oldStatus);
            if (resource != null) {
                resource.setStatus(oldResourceStatus);
            }
            if (reserveSaved) {
                try {
                    FileUtil.saveReserves(records);
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

    private static void saveReserves(List<ReserveRecord> records) {
        try {
            FileUtil.saveReserves(records);
        } catch (IOException e) {
            throw new BusinessException("预约数据保存失败，请检查 reserve.txt。");
        }
    }

    private static void ensureStudent(User user) {
        if (!"student".equals(user.getRole())) {
            throw new PermissionDeniedException("权限不足，仅学生可以办理个人预约。");
        }
    }

    private static String generateResourceId(List<CampusResource> resources) {
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (findResourceById(resources, id).isSuccess());
        return id;
    }

    private static Result<CampusResource> findResourceById(
            List<CampusResource> resources, String id) {
        Optional<CampusResource> found = resources.stream()
                .filter(resource -> id != null && id.equals(resource.getId()))
                .findFirst();
        if (found.isPresent()) {
            return Result.success("查询资源成功。", found.get());
        }
        return Result.fail("未找到该 ID 对应的资源。");
    }

    private static boolean isResourceStatusValid(String status) {
        return "AVAILABLE".equals(status)
                || "OCCUPIED".equals(status)
                || "DISABLED".equals(status);
    }

    private static void ensureAdmin(User user) {
        if (!"admin".equals(user.getRole())) {
            throw new PermissionDeniedException("权限不足，仅管理员可以管理校园资源。");
        }
    }

    private static Result<User> register(List<User> users, String username,
            String password) throws IOException {
        if (!isUsernameValid(username)) {
            throw new ParameterEmptyException("账号不能为空。");
        }
        if (!isPasswordValid(password)) {
            throw new ParameterEmptyException("密码不能为空且长度不能少于6位。");
        }
        if (findUserByUsername(users, username).isSuccess()) {
            throw new UserAlreadyExistsException("账号已存在，注册失败。");
        }

        User user = new User(username, password, "student", LocalDateTime.now());
        users.add(user);
        FileUtil.saveUsers(users);
        return Result.success("注册成功。", user);
    }

    private static Result<User> findUserByUsername(List<User> users, String username) {
        for (User user : users) {
            if (username != null && username.equals(user.getUsername())) {
                return Result.success("查询用户成功。", user);
            }
        }
        return Result.fail("用户不存在。");
    }

    private static boolean isUsernameValid(String username) {
        return username != null && !username.trim().isEmpty();
    }

    private static boolean isPasswordValid(String password) {
        return password != null && !password.isEmpty() && password.length() >= 6;
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
