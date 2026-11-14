package dao;

import entity.CampusResource;
import utils.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileResourceDao implements ResourceDao {
    private static final FileResourceDao INSTANCE;

    static {
        try {
            INSTANCE = new FileResourceDao();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final List<CampusResource> resources;

    private FileResourceDao() throws IOException {
        resources = FileUtil.loadResources();
    }

    public static FileResourceDao getInstance() {
        return INSTANCE;
    }

    @Override
    public List<CampusResource> findAll() {
        return new ArrayList<CampusResource>(resources);
    }

    @Override
    public CampusResource findById(String id) {
        for (CampusResource resource : resources) {
            if (resource.getId().equals(id)) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public void add(CampusResource resource) {
        resources.add(resource);
    }

    @Override
    public void remove(CampusResource resource) {
        resources.remove(resource);
    }

    @Override
    public void save() throws IOException {
        FileUtil.saveResources(resources);
    }
}
