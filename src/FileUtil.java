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
import java.util.List;

// Compiled and verified with Temurin JDK 8 (1.8.0_504).
public final class FileUtil {
    private static final String USER_FILE = "user.txt";
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
                List<String> fields = splitRecord(line);
                if (fields.size() != 4) {
                    throw new IOException("用户数据文件格式错误。");
                }
                users.add(new User(fields.get(0), fields.get(1), fields.get(2),
                        LocalDateTime.parse(fields.get(3))));
            }
        }
        return users;
    }

    public static void saveUsers(List<User> users) throws IOException {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (User user : users) {
                writer.write(escape(user.getUsername()));
                writer.write(SEPARATOR);
                writer.write(escape(user.getPassword()));
                writer.write(SEPARATOR);
                writer.write(escape(user.getRole()));
                writer.write(SEPARATOR);
                writer.write(user.getRegisterTime().toString());
                writer.newLine();
            }
        }
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

    private static List<String> splitRecord(String line) throws IOException {
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
                    throw new IOException("用户数据文件格式错误。");
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
            throw new IOException("用户数据文件格式错误。");
        }
        fields.add(field.toString());
        return fields;
    }
}
