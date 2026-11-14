package dao;

import entity.ReserveRecord;
import java.io.IOException;
import java.util.List;

public interface ReserveRecordDao {
    List<ReserveRecord> findAll();

    ReserveRecord findById(String id);

    void add(ReserveRecord record);

    void remove(ReserveRecord record);

    void save() throws IOException;
}
