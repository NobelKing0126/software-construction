package ui.console;

import dao.DaoFactory;
import dao.ReserveRecordDao;
import dao.ResourceDao;
import dao.UserDao;
import exception.BusinessException;
import service.UserService;
import service.impl.ReserveRecordServiceImpl;
import service.impl.ResourceServiceImpl;
import service.impl.UserServiceImpl;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
final class ConsoleApplication {
    static void start() {
        try {
            loadUsers();
            loadResources();
            loadReserves();
        } catch (BusinessException e) {
            ConsoleOutput.showResult(Result.fail(e.getMessage()));
            return;
        }

        UserService userService = UserServiceImpl.getInstance();
        ResourceConsole resourceConsole = new ResourceConsole(ResourceServiceImpl.getInstance());
        ReserveConsole reserveConsole = new ReserveConsole(ReserveRecordServiceImpl.getInstance());
        new ConsoleSession(userService, resourceConsole, reserveConsole).run();
    }

    private static UserDao loadUsers() {
        try {
            return DaoFactory.getUserDao();
        } catch (Exception | ExceptionInInitializerError e) {
            throw new BusinessException("用户数据加载失败，请检查 user.txt。");
        }
    }

    private static ResourceDao loadResources() {
        try {
            return DaoFactory.getResourceDao();
        } catch (Exception | ExceptionInInitializerError e) {
            throw new BusinessException("资源数据加载失败，请检查 resource.txt。");
        }
    }

    private static ReserveRecordDao loadReserves() {
        try {
            return DaoFactory.getReserveRecordDao();
        } catch (Exception | ExceptionInInitializerError e) {
            throw new BusinessException("预约数据加载失败，请检查 reserve.txt。");
        }
    }
}
