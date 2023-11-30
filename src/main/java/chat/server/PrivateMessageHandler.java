package chat.server;

public class PrivateMessageHandler extends MessageHandler {

    Account participantA;
    Account participantB;

    PrivateMessageHandler(RoomManager rm, Account p1, Account p2) {
        super("PRIVAT: " + p1.name + " & " + p2.name, rm);
        participantA = p1;
        participantB = p2;
    }

    boolean is_participant(Account account) {
        if (participantA == account || participantB == account) {
            return true;
        }
        else {
            return false;
        }
    }

    String get_partner_name(Account requesting) {
        if (requesting == participantA) {
            return participantB.name;
        }
        else {
            return participantA.name;
        }
    }
}
