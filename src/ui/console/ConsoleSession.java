package ui.console;

import java.io.IOException;
import java.util.Scanner;

import entity.User;
import exception.BusinessException;
import service.UserService;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
final class ConsoleSession {
    private final UserService userService;
    private final ResourceConsole resources;
    private final ReserveConsole reserves;

    ConsoleSession(UserService userService, ResourceConsole resources, ReserveConsole reserves) {
        this.userService = userService;
        this.resources = resources;
        this.reserves = reserves;
    }

    void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            ConsoleOutput.showMainMenu();
            String choice = scanner.nextLine();
            try {
                if ("1".equals(choice)) {
                    login(scanner);
                } else if ("2".equals(choice)) {
                    register(scanner);
                } else if ("0".equals(choice)) {
                    System.out.println("已退出系统。");
                    break;
                } else {
                    System.out.println("无效选项，请重新选择。");
                }
            } catch (BusinessException e) {
                ConsoleOutput.showResult(Result.fail(e.getMessage()));
            } catch (IOException e) {
                ConsoleOutput.showResult(Result.fail("用户数据保存失败，请检查 user.txt。"));
            } catch (Exception e) {
                ConsoleOutput.showResult(Result.fail("系统操作失败，请稍后重试。"));
            }
        }
        scanner.close();
    }

    private void login(Scanner scanner) {
        System.out.print("请输入账号：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入密码：");
        String password = scanner.nextLine();
        Result<User> result = userService.login(username, password);
        System.out.println(result.getMsg());
        if (result.isSuccess()) {
            runResourceMenu(scanner, result.getData());
        }
    }

    private void register(Scanner scanner) throws IOException {
        System.out.print("请输入注册账号：");
        String username = scanner.nextLine().trim();
        System.out.print("请输入注册密码：");
        String password = scanner.nextLine();
        ConsoleOutput.showResult(userService.register(username, password));
    }

    private void runResourceMenu(Scanner scanner, User user) {
        userService.requireResourceAccess(user);
        while (true) {
            ConsoleOutput.showResourceMenu("admin".equals(user.getRole()));
            String choice = scanner.nextLine();
            try {
                if ("0".equals(choice)) {
                    System.out.println("已退出登录。");
                    return;
                }
                if (!resources.handleChoice(scanner, user, choice)
                        && !reserves.handleChoice(scanner, user, choice)) {
                    System.out.println("无效选项，请重新选择。");
                }
            } catch (BusinessException e) {
                ConsoleOutput.showResult(Result.fail(e.getMessage()));
            } catch (IOException e) {
                ConsoleOutput.showResult(Result.fail("资源数据保存失败，请检查 resource.txt。"));
            } catch (Exception e) {
                ConsoleOutput.showResult(Result.fail("系统操作失败，请稍后重试。"));
            }
        }
    }
}
