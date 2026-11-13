import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public class SystemMain {
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
                        resourceMenu(scanner, result.getData(), resources);
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
            List<CampusResource> resources) {
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
                System.out.println("0. 退出登录");
            } else {
                System.out.println("学生资源查询菜单");
                System.out.println("1. 查询可预约资源");
                System.out.println("5. 按名称查询资源");
                System.out.println("6. 按 ID 查询资源");
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
                        showResult(readAndModifyResource(scanner, user, resources));
                    } else if ("4".equals(choice)) {
                        showResult(readAndDeleteResource(scanner, user, resources));
                    } else {
                        System.out.println("无效选项，请重新选择。");
                    }
                } else if ("1".equals(choice)) {
                    showResult(queryAvailableResources(user, resources));
                } else if ("2".equals(choice)) {
                    showResult(addResource(user, resources, null, null, null, null));
                } else if ("3".equals(choice)) {
                    showResult(modifyResource(user, resources, null, null, null, null, null));
                } else if ("4".equals(choice)) {
                    showResult(deleteResource(user, resources, null));
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
            List<CampusResource> resources) throws IOException {
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
        return modifyResource(user, resources, id, name, type, location, status);
    }

    private static Result<CampusResource> modifyResource(User user,
            List<CampusResource> resources, String id, String name, String type,
            String location, String status) throws IOException {
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

        CampusResource resource = found.getData();
        resource.setName(name);
        resource.setType(type);
        resource.setLocation(location);
        resource.setStatus(status);
        FileUtil.saveResources(resources);
        return Result.success("资源修改成功。", resource);
    }

    private static Result<Void> readAndDeleteResource(Scanner scanner, User user,
            List<CampusResource> resources) throws IOException {
        System.out.print("请输入要删除的资源 ID：");
        return deleteResource(user, resources, scanner.nextLine().trim());
    }

    private static Result<Void> deleteResource(User user,
            List<CampusResource> resources, String id) throws IOException {
        ensureAdmin(user);
        Result<CampusResource> found = findResourceById(resources, id);
        if (!found.isSuccess()) {
            throw new ResourceNotFoundException("资源不存在，无法删除。");
        }
        resources.remove(found.getData());
        FileUtil.saveResources(resources);
        return Result.success("资源删除成功。");
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
