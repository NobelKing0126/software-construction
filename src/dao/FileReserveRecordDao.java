package dao;

import entity.ReserveRecord;
import utils.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileReserveRecordDao implements ReserveRecordDao {
    private final List<ReserveRecord> records;

    public FileReserveRecordDao() throws IOException {
        records = FileUtil.loadReserves();
    }

    @Override
    public List<ReserveRecord> findAll() {
        return new ArrayList<ReserveRecord>(records);
    }

    @Override
    public ReserveRecord findById(String id) {
        return records.stream()
                .filter(record -> record.getRecordId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public void add(ReserveRecord record) {
        records.add(record);
    }

    @Override
    public void remove(ReserveRecord record) {
        records.remove(record);
    }

    @Override
    public void save() throws IOException {
        FileUtil.saveReserves(records);
    }
}
