package service;

import java.io.IOException;
import java.util.List;

import entity.CampusResource;
import entity.User;
import utils.Result;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public interface ResourceService {
    Result<CampusResource> addResource(User user, String name, String type,
            String location, String description) throws IOException;

    Result<List<CampusResource>> queryAllResources(User user);

    Result<List<CampusResource>> queryAvailableResources(User user);

    Result<List<CampusResource>> queryResourcesByName(User user, String keyword);

    Result<CampusResource> queryResourceById(User user, String id);

    void requireResourceForModification(User user, String id);

    Result<CampusResource> modifyResource(User user, String id, String name,
            String type, String location, String status) throws IOException;

    Result<Void> deleteResource(User user, String id) throws IOException;
}
