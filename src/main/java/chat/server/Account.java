package chat.server;

import java.io.Serializable;

class Account implements Serializable {
    boolean allowed = true;
    String name;
    String password;

    Account(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public void ban() {
        this.allowed = false;
    }

    public void unban() {
        this.allowed = true;
    }

}
