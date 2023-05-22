package classes.sql_managers;

import classes.console.TextColor;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.util.Properties;

public class SQLManager {
    private final static String createMovieTable = """
            CREATE TABLE IF NOT EXISTS movies(
                uuid_id uuid PRIMARY KEY,
                name VARCHAR(255) NOT NULL CHECK (name <> ''),
                coordinateX BIGINT NOT NULL CHECK (coordinateX <= 279 ),
                coordinateY BIGINT NOT NULL CHECK (coordinateY > -230),
                creation_date DATE NOT NULL,
                oscars_count BIGINT NOT NULL CHECK (oscars_count > 0),
                golden_palm_count BIGINT NOT NULL CHECK (golden_palm_count > 0),
                budget FLOAT NOT NULL CHECK (budget > 0) DEFAULT NULL,
                id_mpaarating INTEGER DEFAULT NULL,
                uuid_director uuid NOT NULL REFERENCES directors(uuid_director) ON DELETE CASCADE,
                user_id VARCHAR(255) NOT NULL REFERENCES users(login) ON DELETE CASCADE
            );""";

    private static final String createDirectorsTable = """
            CREATE TABLE IF NOT EXISTS directors (
                uuid_director uuid PRIMARY KEY,
                name VARCHAR(255) NOT NULL CHECK (name <> ''),
                birthday DATE DEFAULT NULL,
                height DOUBLE PRECISION DEFAULT NULL CHECK (height > 0),
                passport_id VARCHAR(255) NOT NULL UNIQUE CHECK (length(passport_id) >= 7 AND passport_id <> ''),
                eye_color INTEGER NOT NULL
            );""";

    private static final String createUserTable = """
            CREATE TABLE IF NOT EXISTS users(
                login VARCHAR(255) PRIMARY KEY,
                pass_hash TEXT NOT NULL,
            );""";

    public static void main(String[] args) {
        initDB();
    }

    static Connection getDBConnection() {
        Connection dbConnection = null;
        Dotenv env = Dotenv.load();
        try {
            String url = "jdbc:postgresql://%s:%d/studs".formatted(env.get("DB_HOST"), Integer.parseInt(env.get("DB_PORT")));
            Properties props = new Properties();
            props.setProperty("user", env.get("DB_USER"));
            props.setProperty("password", env.get("DB_PASS"));
            dbConnection = DriverManager.getConnection(url, props);
        } catch (SQLException e) {
            System.out.println(TextColor.green(e.getMessage()));
        } catch (NumberFormatException e) {
            System.out.println(TextColor.red("Переменная окружения DB_PORT должна быть в формате целого числа 0-65535"));
        }
        return dbConnection;
    }

    public static void execute(String command) {
        try (Connection dbConnection = getDBConnection(); Statement statement = dbConnection.createStatement()) {
            statement.execute(command);
            System.out.println(TextColor.cyan("Обращение к базе данных выполнено"));
        } catch (NullPointerException | SQLException e) {
            e.printStackTrace();
            System.out.println(TextColor.grey("Возникла проблема при обращении к базе данных"));
        }
    }

    public static ResultSet executeQuery(String command) {
        try (Connection dbConnection = getDBConnection(); Statement statement = dbConnection.createStatement()) {
            return statement.executeQuery(command);
        } catch (NullPointerException | SQLException e) {
            e.printStackTrace();
            System.out.println(TextColor.grey("Возникла проблема при обращении к базе данных"));
        }
        return null;
    }
    public static int executeUpdate(String command) {
        try (Connection dbConnection = getDBConnection(); Statement statement = dbConnection.createStatement()) {
            return statement.executeUpdate(command);
        } catch (NullPointerException | SQLException e) {
            e.printStackTrace();
            System.out.println(TextColor.grey("Возникла проблема при обращении к баз данных"));
        }
        return Integer.MAX_VALUE;
    }

    private static void initDB() {
        execute(createDirectorsTable);
        execute(createUserTable);
        execute(createMovieTable);
    }
}
