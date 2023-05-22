package classes;

import classes.abs.NamedCommand;
import classes.console.TextColor;
import classes.sql_managers.SQLManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientSession implements Runnable {
    private final Socket socket;

    public ClientSession(Socket socket) {
        this.socket = socket;
    }


    private UserCredentials authorize(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        String userChoice = "";
        while (!(userChoice.equals("1") || userChoice.equals("2"))) {
            out.writeUTF("1 - Войти (по умолчанию)\n2 - Зарегистрироваться");
            out.flush();
            userChoice = in.readUTF();
        }
        if (userChoice.equals("2"))
            return register(in, out);
        else return login(in, out);
    }

    private UserCredentials login(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        while (true) {
            out.writeUTF("Логин: ");
            out.flush();
            String login = in.readUTF();
            out.writeUTF("Пароль: ");
            out.flush();
            String password = in.readUTF();

            if (checkLogin(login)) {
                UserCredentials userCredentials = new UserCredentials(login, password);
                if (checkPassword(userCredentials)){
                 return userCredentials;
                }

                else System.out.println(TextColor.red("Неверный пароль\n"));
            } else System.out.println(TextColor.yellow("Пользователь с таким логином не существует\n"));
        }
    }

    private boolean checkPassword(UserCredentials userCredentials) {
        ResultSet loginResultSet = SQLManager.executeQuery("SELECT * FROM users where login=" + userCredentials.getUsername());
        try {
            if (loginResultSet.next()){
                String hashedPassword = loginResultSet.getString("pass_hash");
                if (hashedPassword.equals(userCredentials.getHashedPassword())) return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }



        return false;
    }

    private boolean checkLogin(String login) {
        try {
            ResultSet loginResultSet = SQLManager.executeQuery("SELECT * FROM users where login=" + login);
            if(loginResultSet.next()) return true;
        } catch (SQLException e) {
            System.out.println(TextColor.grey("Возникла проблема при обращении к базе данных"));
        }
        return false;
    }

    private UserCredentials register(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        // null - if it doesn't success ; usercredits if success
        UserCredentials userCredentials = null;

            out.writeUTF("Логин: ");
            out.flush();
            String login = in.readUTF();
            out.writeUTF("Пароль: ");
            out.flush();
            String password = in.readUTF();

            if (!checkLogin(login)) {
                userCredentials = new UserCredentials(login, password);
            } else System.out.println(TextColor.yellow("Пользователь с таким логином уже существует\n"));


        return userCredentials;
    }

    @Override
    public void run() {
        try (
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())
        ) {
            System.out.println(TextColor.grey("Соединение установлено: " + socket.getInetAddress()));
            UserCredentials currentUserID = authorize(inputStream, outputStream);

            while (!socket.isClosed()) {
                Object inputObject = inputStream.readObject();
                if (inputObject instanceof NamedCommand command) {
                    Response outputData;
                    if (command.hasTransferData()) {
                        Object inputData = inputStream.readObject();
                        outputData = command.execute(inputData);
                    } else
                        outputData = command.execute(null);
                    if (outputData != null)
                        outputStream.writeObject(outputData);
                } else
                    outputStream.writeObject(new Response(1).setData(TextColor.yellow("Передана неизвестная команда")));
                outputStream.flush();
            }
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            System.out.println(TextColor.grey("Соединение разорвано, ожидаю нового подключения"));
        }
    }
}
