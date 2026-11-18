package dao;

import dao.impl.FileReserveRecordDao;
import dao.impl.FileResourceDao;
import dao.impl.FileUserDao;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public final class DaoFactory {
    private DaoFactory() {
    }

    public static UserDao getUserDao() {
        return FileUserDao.getInstance();
    }

    public static ResourceDao getResourceDao() {
        return FileResourceDao.getInstance();
    }

    public static ReserveRecordDao getReserveRecordDao() {
        return FileReserveRecordDao.getInstance();
    }
}
