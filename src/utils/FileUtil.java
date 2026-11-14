package utils;

import entity.CampusResource;
import entity.ReserveRecord;
import entity.ReserveStatus;
import entity.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public final class FileUtil {
    private static final String USER_FILE = "user.txt";
    private static final String RESOURCE_FILE = "resource.txt";
    private static final String RESERVE_FILE = "reserve.txt";
    private static final char SEPARATOR = '|';

    private FileUtil() {
    }

    public static List<User> loadUsers() throws IOException {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }

        List<User> users = new ArrayList<User>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> fields = splitRecord(line, "用户数据文件格式错误。");
                if (fields.size() != 4) {
                    throw new IOException("用户数据文件格式错误。");
                }
                users.add(new User(fields.get(0), fields.get(1), fields.get(2),
                        LocalDateTime.parse(fields.get(3))));
            }
        }
        return users;
    }

    public static List<CampusResource> loadResources() throws IOException {
        File file = new File(RESOURCE_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }

        List<CampusResource> resources = new ArrayList<CampusResource>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> fields = splitRecord(line, "资源数据文件格式错误。");
                if (fields.size() != 6) {
                    throw new IOException("资源数据文件格式错误。");
                }
                resources.add(new CampusResource(fields.get(0), fields.get(1),
                        fields.get(2), fields.get(3), fields.get(4), fields.get(5)));
            }
        }
        return resources;
    }

    public static List<ReserveRecord> loadReserves() throws IOException {
        File file = new File(RESERVE_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }

        List<ReserveRecord> records = new ArrayList<ReserveRecord>();
        Set<String> ids = new HashSet<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                records.add(parseReserveRecord(line, ids));
            }
        }
        return records;
    }

    private static ReserveRecord parseReserveRecord(String line, Set<String> ids) throws IOException {
        List<String> fields = splitRecord(line, "预约数据文件格式错误。");
        if ((fields.size() != 8 && fields.size() != 10) || fields.get(0).isEmpty()
                || !ids.add(fields.get(0))) {
            throw new IOException("预约数据文件格式错误。");
        }
        try {
            ReserveRecord record = new ReserveRecord(fields.get(0), fields.get(1),
                    fields.get(2), LocalDateTime.parse(fields.get(3)),
                    LocalDateTime.parse(fields.get(4)), LocalDateTime.parse(fields.get(5)),
                    ReserveStatus.valueOf(fields.get(6)), fields.get(7));
            // V3.1 的八字段记录没有审核信息，保留空值，不补造历史时间。
            if (fields.size() == 10) {
                record.setRejectReason(fields.get(8));
                if (!fields.get(9).isEmpty()) {
                    record.setAuditTime(LocalDateTime.parse(fields.get(9)));
                }
            }
            return record;
        } catch (RuntimeException e) {
            throw new IOException("预约数据文件格式错误。", e);
        }
    }

    public static void saveUsers(List<User> users) throws IOException {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (User user : users) {
                writeRecord(writer, user.getUsername(), user.getPassword(), user.getRole(),
                        user.getRegisterTime().toString());
            }
        }
    }

    public static void saveResources(List<CampusResource> resources) throws IOException {
        File file = new File(RESOURCE_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (CampusResource resource : resources) {
                writeRecord(writer, resource.getId(), resource.getName(), resource.getType(),
                        resource.getStatus(), resource.getLocation(), resource.getDescription());
            }
        }
    }

    public static void saveReserves(List<ReserveRecord> records) throws IOException {
        File file = new File(RESERVE_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (ReserveRecord record : records) {
                writeRecord(writer, record.getRecordId(), record.getResourceId(),
                        record.getApplicantUsername(), record.getApplyTime().toString(),
                        record.getReserveStartTime().toString(), record.getReserveEndTime().toString(),
                        record.getStatus().name(), record.getRemark() == null ? "" : record.getRemark(),
                        record.getRejectReason() == null ? "" : record.getRejectReason(),
                        record.getAuditTime() == null ? "" : record.getAuditTime().toString());
            }
        }
    }

    private static void writeRecord(BufferedWriter writer, String... fields) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                writer.write(SEPARATOR);
            }
            writer.write(escape(fields[i]));
        }
        writer.newLine();
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '\\') {
                escaped.append("\\\\");
            } else if (character == SEPARATOR) {
                escaped.append('\\').append(SEPARATOR);
            } else if (character == '\r') {
                escaped.append("\\r");
            } else if (character == '\n') {
                escaped.append("\\n");
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    private static List<String> splitRecord(String line, String errorMessage) throws IOException {
        List<String> fields = new ArrayList<String>();
        StringBuilder field = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (escaped) {
                if (character == 'r') {
                    field.append('\r');
                } else if (character == 'n') {
                    field.append('\n');
                } else if (character == SEPARATOR || character == '\\') {
                    field.append(character);
                } else {
                    throw new IOException(errorMessage);
                }
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == SEPARATOR) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(character);
            }
        }

        if (escaped) {
            throw new IOException(errorMessage);
        }
        fields.add(field.toString());
        return fields;
    }
}
