package ui.console;

import java.io.IOException;
import java.util.Scanner;

import entity.CampusResource;
import entity.User;
import service.ResourceService;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
final class ResourceConsole {
    private final ResourceService resourceService;

    ResourceConsole(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    // 返回是否识别该选项；未识别时不读取输入或调用 Service。
    boolean handleChoice(Scanner scanner, User user, String choice) throws IOException {
        Result<?> result;
        switch (choice) {
            case "1":
                result = "admin".equals(user.getRole())
                        ? readAndAddResource(scanner, user)
                        : resourceService.queryAvailableResources(user);
                break;
            case "2":
                result = "admin".equals(user.getRole())
                        ? resourceService.queryAllResources(user)
                        : resourceService.addResource(user, null, null, null, null);
                break;
            case "3":
                result = "admin".equals(user.getRole())
                        ? readAndModifyResource(scanner, user)
                        : resourceService.modifyResource(user, null, null, null, null, null);
                break;
            case "4":
                result = "admin".equals(user.getRole())
                        ? readAndDeleteResource(scanner, user)
                        : resourceService.deleteResource(user, null);
                break;
            case "5":
                System.out.print("请输入资源名称关键词：");
                result = resourceService.queryResourcesByName(user, scanner.nextLine().trim());
                break;
            case "6":
                System.out.print("请输入资源 ID：");
                result = resourceService.queryResourceById(user, scanner.nextLine().trim());
                break;
            default:
                return false;
        }
        ConsoleOutput.showResult(result);
        return true;
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
}
