package lt.satsyuk.api.integrationtest;

public class TestPostgresContainer {

    private static Object instance;

    public static Object getInstance() {
        if (instance == null) {
            try {
                Class<?> cls = Class.forName("org.testcontainers.containers.PostgreSQLContainer");
                Object container = cls.getDeclaredConstructor(String.class).newInstance("postgres:16");

                container = cls.getMethod("withDatabaseName", String.class).invoke(container, "appdb");
                container = cls.getMethod("withUsername", String.class).invoke(container, "app");
                container = cls.getMethod("withPassword", String.class).invoke(container, "app");
                container = cls.getMethod("withReuse", boolean.class).invoke(container, true);

                // start()
                cls.getMethod("start").invoke(container);

                instance = container;
            } catch (Exception e) {
                // If Docker/Testcontainers is not available, avoid throwing during static init.
                // Return null so tests can skip themselves using assumptions.
                instance = null;
            }
        }
        return instance;
    }
}
