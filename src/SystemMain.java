import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public class SystemMain {
    public static void main(String[] args) {
        List<User> users;
        try {
            users = FileUtil.loadUsers();
        } catch (Exception e) {
            System.out.println("用户数据加载失败，请检查数据文件。");
            return;
        }
        List<CampusResource> resources;
        try {
            resources = FileUtil.loadResources();
        } catch (Exception e) {
            System.out.println("资源数据加载失败，请检查 resource.txt。");
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
                    resourceMenu(scanner, login(scanner, users), resources);
                } else if ("2".equals(choice)) {
                    register(scanner, users);
                } else if ("0".equals(choice)) {
                    System.out.println("已退出系统。");
                    break;
                } else {
                    System.out.println("无效选项，请重新选择。");
                }
            } catch (BusinessException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                System.out.println("系统操作失败，请稍后重试。");
            }
        }

        scanner.close();
    }

    private static User login(Scanner scanner, List<User> users) {
        System.out.print("请输入账号：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入密码：");
        String password = scanner.nextLine();
        if (!isUsernameValid(username)) {
            throw new ParameterEmptyException("账号不能为空。");
        }
        if (password.isEmpty()) {
            throw new ParameterEmptyException("密码不能为空。");
        }
        User user = findUserByUsername(users, username);

        if (user == null) {
            throw new UserNotFoundException("用户不存在，登录失败。");
        }
        if (!password.equals(user.getPassword())) {
            throw new PasswordErrorException("密码错误，登录失败。");
        }
        System.out.println("登录成功，欢迎使用校园资源预约系统！");
        return user;
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
                System.out.println("0. 退出登录");
            } else {
                System.out.println("学生资源查询菜单");
                System.out.println("1. 查询可预约资源");
                System.out.println("0. 退出登录");
            }
            System.out.print("请选择：");
            String choice = scanner.nextLine();

            try {
                if ("0".equals(choice)) {
                    System.out.println("已退出登录。");
                    return;
                }
                if (admin) {
                    if ("1".equals(choice)) {
                        addResource(user, scanner, resources);
                    } else if ("2".equals(choice)) {
                        printAllResources(user, resources);
                    } else if ("3".equals(choice)) {
                        modifyResource(user, scanner, resources);
                    } else if ("4".equals(choice)) {
                        deleteResource(user, scanner, resources);
                    } else {
                        System.out.println("无效选项，请重新选择。");
                    }
                } else if ("1".equals(choice)) {
                    printAvailableResources(user, resources);
                } else if ("2".equals(choice)) {
                    addResource(user, scanner, resources);
                } else if ("3".equals(choice)) {
                    modifyResource(user, scanner, resources);
                } else if ("4".equals(choice)) {
                    deleteResource(user, scanner, resources);
                } else {
                    System.out.println("无效选项，请重新选择。");
                }
            } catch (BusinessException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println("资源数据保存失败，请检查 resource.txt。");
            } catch (Exception e) {
                System.out.println("系统操作失败，请稍后重试。");
            }
        }
    }

    private static void addResource(User user, Scanner scanner,
            List<CampusResource> resources) throws IOException {
        ensureAdmin(user);
        System.out.print("请输入资源名称：");
        String name = scanner.nextLine().trim();
        System.out.print("请输入资源类型：");
        String type = scanner.nextLine().trim();
        System.out.print("请输入资源位置：");
        String location = scanner.nextLine().trim();
        System.out.print("请输入资源描述：");
        String description = scanner.nextLine().trim();
        if (name.isEmpty()) {
            throw new ParameterEmptyException("资源名称不能为空。");
        }

        String id = generateResourceId(resources);
        resources.add(new CampusResource(id, name, type, "AVAILABLE", location, description));
        FileUtil.saveResources(resources);
        System.out.println("资源新增成功，资源 ID：" + id);
    }

    private static void printAllResources(User user, List<CampusResource> resources) {
        ensureAdmin(user);
        if (resources.isEmpty()) {
            System.out.println("当前没有校园资源。");
            return;
        }
        for (CampusResource resource : resources) {
            System.out.println(resource);
        }
    }

    private static void printAvailableResources(User user,
            List<CampusResource> resources) {
        if (!"student".equals(user.getRole())) {
            throw new PermissionDeniedException("只有学生可以查询可预约资源。");
        }
        boolean found = false;
        for (CampusResource resource : resources) {
            if ("AVAILABLE".equals(resource.getStatus())) {
                System.out.println(resource);
                found = true;
            }
        }
        if (!found) {
            System.out.println("当前没有可预约资源。");
        }
    }

    private static void modifyResource(User user, Scanner scanner,
            List<CampusResource> resources) throws IOException {
        ensureAdmin(user);
        System.out.print("请输入要修改的资源 ID：");
        CampusResource resource = findResourceById(resources, scanner.nextLine().trim());
        if (resource == null) {
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
        if (name.isEmpty()) {
            throw new ParameterEmptyException("资源名称不能为空。");
        }
        if (!isResourceStatusValid(status)) {
            throw new InvalidResourceStatusException(
                    "资源状态不合法，只能是 AVAILABLE、OCCUPIED 或 DISABLED。");
        }

        resource.setName(name);
        resource.setType(type);
        resource.setLocation(location);
        resource.setStatus(status);
        FileUtil.saveResources(resources);
        System.out.println("资源修改成功。");
    }

    private static void deleteResource(User user, Scanner scanner,
            List<CampusResource> resources) throws IOException {
        ensureAdmin(user);
        System.out.print("请输入要删除的资源 ID：");
        CampusResource resource = findResourceById(resources, scanner.nextLine().trim());
        if (resource == null) {
            throw new ResourceNotFoundException("资源不存在，无法删除。");
        }
        resources.remove(resource);
        FileUtil.saveResources(resources);
        System.out.println("资源删除成功。");
    }

    private static String generateResourceId(List<CampusResource> resources) {
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (findResourceById(resources, id) != null);
        return id;
    }

    private static CampusResource findResourceById(List<CampusResource> resources,
            String id) {
        for (CampusResource resource : resources) {
            if (id.equals(resource.getId())) {
                return resource;
            }
        }
        return null;
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

    private static void register(Scanner scanner, List<User> users) throws IOException {
        System.out.print("请输入注册账号：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入注册密码：");
        String password = scanner.nextLine();

        if (!isUsernameValid(username)) {
            throw new ParameterEmptyException("账号不能为空。");
        }
        if (!isPasswordValid(password)) {
            throw new ParameterEmptyException("密码不能为空且长度不能少于6位。");
        }
        if (findUserByUsername(users, username) != null) {
            throw new UserAlreadyExistsException("账号已存在，注册失败。");
        }

        User user = new User(username, password, "student", LocalDateTime.now());
        users.add(user);
        FileUtil.saveUsers(users);
        System.out.println("注册成功。");
        System.out.println(user);
    }

    private static User findUserByUsername(List<User> users, String username) {
        for (User user : users) {
            if (username.equals(user.getUsername())) {
                return user;
            }
        }
        return null;
    }

    private static boolean isUsernameValid(String username) {
        return username != null && !username.trim().isEmpty();
    }

    private static boolean isPasswordValid(String password) {
        return password != null && !password.isEmpty() && password.length() >= 6;
    }
}
