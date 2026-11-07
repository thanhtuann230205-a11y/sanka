import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.internal.resolver.ChecksumCalculator;
import org.flywaydb.core.internal.resource.filesystem.FileSystemResource;

/** Offline checks; no cloud connection or application-local.yml is used. */
public class MigrationToolCheck {
    public static void main(String[] args) throws Exception {
        for (String name : new String[]{"V1__schema.sql", "V2__sample_content.sql", "V3__assessment_workflow.sql"}) {
            Path path = Path.of("src/main/resources/db/migration", name);
            int actual = ChecksumCalculator.calculate(new FileSystemResource(
                    new Location("filesystem:src/main/resources/db/migration"), path.toString(), StandardCharsets.UTF_8, false));
            if (MigrateCloud.checksum(path) != actual) throw new AssertionError("Checksum mismatch: " + name);
        }
        var recover = MigrateCloud.class.getDeclaredMethod("recoverV3", java.sql.Connection.class);
        recover.setAccessible(true);
        try (var c = DriverManager.getConnection("jdbc:h2:mem:operator_check")) {
            try {
                recover.invoke(null, c);
                throw new AssertionError("Recovery accepted an unrelated database.");
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof IllegalStateException)
                        || !"Unexpected database.".equals(e.getCause().getMessage())) throw e;
            }
        }
        try (var c = DriverManager.getConnection("jdbc:h2:mem:recovery_checks")) {
            c.createStatement().execute("CREATE TABLE flyway_schema_history(installed_rank INT, version VARCHAR, success BOOLEAN, checksum INT, script VARCHAR)");
            String[] scripts = {"V1__schema.sql", "V2__sample_content.sql", "V3__assessment_workflow.sql"};
            for (int i = 0; i < scripts.length; i++) {
                try (var p = c.prepareStatement("INSERT INTO flyway_schema_history VALUES(?,?,?,?,?)")) {
                    p.setInt(1, i+1); p.setString(2, ""+(i+1)); p.setBoolean(3, i < 2);
                    p.setInt(4, MigrateCloud.checksum(Path.of("src/main/resources/db/migration", scripts[i])));
                    p.setString(5, scripts[i]); p.executeUpdate();
                }
            }
            c.createStatement().execute("CREATE TABLE students(id INT)");
            c.createStatement().execute("INSERT INTO students VALUES(1)");
            var proxy = (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(
                    MigrationToolCheck.class.getClassLoader(), new Class[]{java.sql.Connection.class},
                    (ignored, method, values) -> method.getName().equals("getCatalog") ? "sanka_lms" : method.invoke(c, values));
            try {
                recover.invoke(null, proxy);
                throw new AssertionError("Recovery accepted user data.");
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof IllegalStateException)
                        || !e.getCause().getMessage().contains("contains data")) throw e;
            }
            try (var r = c.createStatement().executeQuery("SELECT success FROM flyway_schema_history WHERE version='3'")) {
                r.next(); if (r.getBoolean(1)) throw new AssertionError("Refused recovery modified history.");
            }
        }
        System.out.println("PASS: checksums match Flyway; unrelated databases and user data refused; failed history preserved.");
    }
}
