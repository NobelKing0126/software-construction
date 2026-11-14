package dao;

import entity.CampusResource;
import java.io.IOException;
import java.util.List;

public interface ResourceDao {
    List<CampusResource> findAll();

    CampusResource findById(String id);

    void add(CampusResource resource);

    void remove(CampusResource resource);

    void save() throws IOException;
}
