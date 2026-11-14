package ui.console;

import dao.FileReserveRecordDao;
import dao.FileResourceDao;
import dao.FileUserDao;
import dao.ReserveRecordDao;
import dao.ResourceDao;
import dao.UserDao;
import exception.BusinessException;
import service.ReserveRecordService;
import service.ResourceService;
import service.UserService;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
final class ConsoleApplication {
    static void start() {
        UserDao users;
        ResourceDao resources;
        ReserveRecordDao records;
        try {
            users = loadUsers();
            resources = loadResources();
            records = loadReserves();
        } catch (BusinessException e) {
            ConsoleOutput.showResult(Result.fail(e.getMessage()));
            return;
        }

        UserService userService = new UserService(users);
        ResourceConsole resourceConsole = new ResourceConsole(new ResourceService(resources, records));
        ReserveConsole reserveConsole = new ReserveConsole(new ReserveRecordService(records, resources));
        new ConsoleSession(userService, resourceConsole, reserveConsole).run();
    }

    private static UserDao loadUsers() {
        try {
            return new FileUserDao();
        } catch (Exception e) {
            throw new BusinessException("用户数据加载失败，请检查 user.txt。");
        }
    }

    private static ResourceDao loadResources() {
        try {
            return new FileResourceDao();
        } catch (Exception e) {
            throw new BusinessException("资源数据加载失败，请检查 resource.txt。");
        }
    }

    private static ReserveRecordDao loadReserves() {
        try {
            return new FileReserveRecordDao();
        } catch (Exception e) {
            throw new BusinessException("预约数据加载失败，请检查 reserve.txt。");
        }
    }
}
