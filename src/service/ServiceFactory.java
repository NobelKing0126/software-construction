package service;

import service.impl.ReserveRecordServiceImpl;
import service.impl.ResourceServiceImpl;
import service.impl.UserServiceImpl;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public final class ServiceFactory {
    private ServiceFactory() {
    }

    public static UserService getUserService() {
        return UserServiceImpl.getInstance();
    }

    public static ResourceService getResourceService() {
        return ResourceServiceImpl.getInstance();
    }

    public static ReserveRecordService getReserveRecordService() {
        return ReserveRecordServiceImpl.getInstance();
    }
}
