import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.zip.CRC32;
import org.flywaydb.core.Flyway;

/** Operator tool: run outside the web container startup deadline. */
public class MigrateCloud {
    public static void main(String[] args) throws Exception {
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Run in an interactive PowerShell terminal.");
        String url = console.readLine("TiDB JDBC URL: ").trim();
        String user = console.readLine("TiDB username (including prefix): ").trim();
        String password = System.getenv("SANKA_MIGRATION_PASSWORD");
        if (password == null || password.isEmpty()) {
            char[] secret = console.readPassword("TiDB password (hidden): ");
            if (secret == null) throw new IllegalStateException("Password input cancelled.");
            password = new String(secret);
            Arrays.fill(secret, '\0');
        } else {
            console.printf("Using password captured from clipboard (hidden).%n");
        }
        if (!url.startsWith("jdbc:mysql://") || !url.contains("/sanka_lms?")
                || !url.contains("sslMode=VERIFY_IDENTITY")) {
            throw new IllegalArgumentException("Use the sanka_lms JDBC URL with sslMode=VERIFY_IDENTITY.");
        }
        Flyway flyway = Flyway.configure().dataSource(url, user, password)
                .locations("filesystem:src/main/resources/db/migration").cleanDisabled(true).load();
        if (Arrays.asList(args).contains("--recover-v3")) {
            try (Connection c = DriverManager.getConnection(url, user, password)) {
                recoverV3(c);
            }
        }
        flyway.migrate();
        flyway.validate();
        System.out.println("SUCCESS: all migrations applied and validated. You can redeploy Vercel.");
    }

    private static void recoverV3(Connection c) throws Exception {
        String schema = c.getCatalog();
        require("sanka_lms".equals(schema), "Unexpected database.");
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(
                "SELECT version, success, checksum, script FROM flyway_schema_history ORDER BY installed_rank")) {
            int count = 0;
            while (r.next()) {
                count++;
                require(Integer.toString(count).equals(r.getString(1)), "Unexpected migration history.");
                require(r.getBoolean(2) == (count < 3), "Expected successful V1/V2 and failed V3 only.");
                String script = switch (count) {
                    case 1 -> "V1__schema.sql";
                    case 2 -> "V2__sample_content.sql";
                    case 3 -> "V3__assessment_workflow.sql";
                    default -> throw new IllegalStateException("Unexpected later migration.");
                };
                require(script.equals(r.getString(4)) && r.getInt(3) == checksum(Path.of("src/main/resources/db/migration", script)),
                        "Migration source/checksum differs: " + script);
                if (count == 3) {
                    require("V3__assessment_workflow.sql".equals(r.getString(4)), "Unexpected V3 script.");
                    require(r.getInt(3) == checksum(Path.of("src/main/resources/db/migration/V3__assessment_workflow.sql")),
                            "V3 checksum differs from the local source. Stop and investigate.");
                }
            }
            require(count == 3, "Recovery is only for the initial failed V3 deployment.");
        }
        // This deliberately refuses databases with user records: no data correction is guessed.
        for (String table : List.of("students", "exam_attempts", "practice_submissions")) {
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
                r.next(); require(r.getLong(1) == 0, "Table " + table + " contains data. Automatic recovery refused.");
            }
        }
        columns(c, schema, "students", "id:bigint:0,email:varchar:190,password:varchar:255,name:varchar:80,major:varchar:32,level:varchar:32,placement_completed:tinyint:0,role:varchar:20", Set.of("major", "level"));
        columns(c, schema, "exam_attempts", "id:bigint:0,student_id:bigint:0,major:varchar:32,stage:varchar:32,score:int:0,passed:tinyint:0,evidence:text:0,created_at:datetime:0", Set.of());
        columns(c, schema, "practice_submissions", "id:bigint:0,student_id:bigint:0,major:varchar:32,kind:varchar:32,response:text:0,status:varchar:20,language_score:int:0,technical_score:int:0,communication_score:int:0,task_score:int:0,feedback:text:0,reviewer_id:bigint:0,created_at:datetime:0,reviewed_at:datetime:0", Set.of("language_score", "technical_score", "communication_score", "task_score", "feedback", "reviewer_id", "reviewed_at"));
        try (PreparedStatement p = c.prepareStatement("SELECT COLUMN_NAME,COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME='students' AND COLUMN_NAME IN ('role','placement_completed')")) {
            p.setString(1, schema);
            try (ResultSet r = p.executeQuery()) {
                int count = 0;
                while (r.next()) {
                    count++;
                    require((r.getString(1).equals("role") ? "STUDENT" : "0").equals(r.getString(2)), "Unexpected student column default.");
                }
                require(count == 2, "Missing student defaults.");
            }
        }
        index(c, schema, "exam_attempts", "idx_attempt_student", "student_id,major,stage");
        index(c, schema, "practice_submissions", "idx_submission_student", "student_id,major,kind");
        foreignKey(c, schema, "exam_attempts", "student_id", "fk_attempt_student");
        foreignKey(c, schema, "practice_submissions", "student_id", "fk_submission_student");
        foreignKey(c, schema, "practice_submissions", "reviewer_id", "fk_submission_reviewer");
        // Every V3 DDL effect is present. Its UPDATE is vacuous because students is empty.
        // Preserve the original script/checksum/rank; acknowledge only this completed failed entry.
        try (Statement s = c.createStatement()) {
            require(s.executeUpdate("UPDATE flyway_schema_history SET success=1 WHERE version='3' AND success=0 AND script='V3__assessment_workflow.sql'") == 1,
                    "Migration history changed concurrently. Stop other deployments first.");
        }
        System.out.println("Verified completed V3 on an empty database; recovered its history entry.");
    }

    private static void columns(Connection c, String schema, String table, String specification, Set<String> nullable) throws Exception {
        Map<String, String[]> expected = new HashMap<>();
        for (String entry : specification.split(",")) {
            String[] fields = entry.split(":"); expected.put(fields[0], fields);
        }
        try (PreparedStatement p = c.prepareStatement("SELECT COLUMN_NAME,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH,IS_NULLABLE,DATETIME_PRECISION,COLUMN_KEY,EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME=?")) {
            p.setString(1, schema); p.setString(2, table);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    String name = r.getString(1); String[] e = expected.remove(name);
                    require(e != null && e[1].equalsIgnoreCase(r.getString(2)), "Unexpected column/type: " + table + "." + name);
                    if (e[1].equals("varchar")) require(r.getInt(3) == Integer.parseInt(e[2]), "Wrong varchar length.");
                    require(nullable.contains(name) == "YES".equals(r.getString(4)), "Wrong nullability: " + name);
                    if (e[1].equals("datetime")) require(r.getInt(5) == 6, "Wrong datetime precision.");
                    if (name.equals("id")) require("PRI".equals(r.getString(6)) && r.getString(7).contains("auto_increment"), "Wrong primary key.");
                }
            }
        }
        require(expected.isEmpty(), "Missing columns in " + table + ": " + expected.keySet());
    }

    private static void index(Connection c, String schema, String table, String name, String expected) throws Exception {
        List<String> names = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT COLUMN_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND INDEX_NAME=? ORDER BY SEQ_IN_INDEX")) {
            p.setString(1, schema); p.setString(2, table); p.setString(3, name);
            try (ResultSet r = p.executeQuery()) { while (r.next()) names.add(r.getString(1)); }
        }
        require(expected.equals(String.join(",", names)), "Missing or different index: " + name);
    }

    private static void foreignKey(Connection c, String schema, String table, String column, String name) throws Exception {
        try (PreparedStatement p = c.prepareStatement("SELECT COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=? AND TABLE_NAME=? AND CONSTRAINT_NAME=?")) {
            p.setString(1, schema); p.setString(2, table); p.setString(3, name);
            try (ResultSet r = p.executeQuery()) {
                require(r.next() && column.equals(r.getString(1)) && "students".equals(r.getString(2)) && "id".equals(r.getString(3)) && !r.next(), "Missing or different foreign key: " + name);
            }
        }
    }

    static int checksum(Path path) throws Exception {
        CRC32 crc = new CRC32();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.startsWith("\uFEFF")) line = line.substring(1);
            crc.update(line.getBytes(StandardCharsets.UTF_8));
        }
        return (int) crc.getValue();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
