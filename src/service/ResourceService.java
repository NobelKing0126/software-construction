package service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dao.ReserveRecordDao;
import dao.ResourceDao;
import entity.CampusResource;
import entity.ReserveStatus;
import entity.User;
import exception.BusinessException;
import exception.InvalidResourceStatusException;
import exception.ParameterEmptyException;
import exception.PermissionDeniedException;
import exception.ResourceNotFoundException;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public class ResourceService {
    private final ResourceDao resources;
    private final ReserveRecordDao records;

    public ResourceService(ResourceDao resources, ReserveRecordDao records) {
        this.resources = resources;
        this.records = records;
    }

    public Result<CampusResource> addResource(User user, String name, String type,
            String location, String description) throws IOException {
        ensureAdmin(user);
        if (name == null || name.isEmpty()) {
            throw new ParameterEmptyException("资源名称不能为空。");
        }

        String id = generateResourceId();
        CampusResource resource = new CampusResource(id, name, type, "AVAILABLE", location,
                description);
        resources.add(resource);
        resources.save();
        return Result.success("资源新增成功，资源 ID：" + id, resource);
    }

    public Result<List<CampusResource>> queryAllResources(User user) {
        ensureAdmin(user);
        return collectResourceQuery(resources.findAll().stream(),
                "查询全部资源成功。", "当前没有校园资源。");
    }

    public Result<List<CampusResource>> queryAvailableResources(User user) {
        if (!"student".equals(user.getRole())) {
            throw new PermissionDeniedException("只有学生可以查询可预约资源。");
        }
        return collectResourceQuery(resources.findAll().stream()
                .filter(resource -> "AVAILABLE".equals(resource.getStatus())),
                "查询可预约资源成功。", "当前没有可预约资源。");
    }

    public Result<List<CampusResource>> queryResourcesByName(User user, String keyword) {
        boolean admin = "admin".equals(user.getRole());
        if (!admin && !"student".equals(user.getRole())) {
            throw new PermissionDeniedException("当前账号角色无资源操作权限。");
        }
        if (keyword == null || keyword.isEmpty()) {
            throw new ParameterEmptyException("资源名称关键词不能为空。");
        }
        return collectResourceQuery(resources.findAll().stream()
                .filter(resource -> admin || "AVAILABLE".equals(resource.getStatus()))
                .filter(resource -> resource.getName().contains(keyword)),
                "按名称查询资源成功。", "未找到匹配的资源。");
    }

    public Result<CampusResource> queryResourceById(User user, String id) {
        if (!"admin".equals(user.getRole()) && !"student".equals(user.getRole())) {
            throw new PermissionDeniedException("当前账号角色无资源操作权限。");
        }
        Result<CampusResource> found = findResourceById(id);
        if ("student".equals(user.getRole()) && found.isSuccess()
                && !"AVAILABLE".equals(found.getData().getStatus())) {
            return Result.fail("未找到该 ID 对应的资源。");
        }
        return found;
    }

    public void requireResourceForModification(User user, String id) {
        ensureAdmin(user);
        if (!findResourceById(id).isSuccess()) {
            throw new ResourceNotFoundException("资源不存在，无法修改。");
        }
    }

    public Result<CampusResource> modifyResource(User user, String id, String name,
            String type, String location, String status) throws IOException {
        ensureAdmin(user);
        Result<CampusResource> found = findResourceById(id);
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
        if (!"OCCUPIED".equals(status) && records.findAll().stream()
                .anyMatch(record -> id.equals(record.getResourceId())
                        && record.getStatus() == ReserveStatus.PASS)) {
            throw new BusinessException("资源存在已通过预约，状态必须保持 OCCUPIED。");
        }

        CampusResource resource = found.getData();
        resource.setName(name);
        resource.setType(type);
        resource.setLocation(location);
        resource.setStatus(status);
        resources.save();
        return Result.success("资源修改成功。", resource);
    }

    public Result<Void> deleteResource(User user, String id) throws IOException {
        ensureAdmin(user);
        Result<CampusResource> found = findResourceById(id);
        if (!found.isSuccess()) {
            throw new ResourceNotFoundException("资源不存在，无法删除。");
        }
        if (records.findAll().stream().anyMatch(record -> id.equals(record.getResourceId())
                && record.getStatus() == ReserveStatus.PASS)) {
            throw new BusinessException("存在已通过预约，不能删除资源。");
        }
        resources.remove(found.getData());
        resources.save();
        return Result.success("资源删除成功。");
    }

    private Result<List<CampusResource>> collectResourceQuery(
            Stream<CampusResource> stream, String successMsg, String emptyMsg) {
        List<CampusResource> matched = stream
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
        return Result.success(matched.isEmpty() ? emptyMsg : successMsg, matched);
    }

    private Result<CampusResource> findResourceById(String id) {
        CampusResource found = resources.findById(id);
        if (found != null) {
            return Result.success("查询资源成功。", found);
        }
        return Result.fail("未找到该 ID 对应的资源。");
    }

    private String generateResourceId() {
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (resources.findById(id) != null);
        return id;
    }

    private boolean isResourceStatusValid(String status) {
        return "AVAILABLE".equals(status)
                || "OCCUPIED".equals(status)
                || "DISABLED".equals(status);
    }

    private void ensureAdmin(User user) {
        if (!"admin".equals(user.getRole())) {
            throw new PermissionDeniedException("权限不足，仅管理员可以管理校园资源。");
        }
    }
}
