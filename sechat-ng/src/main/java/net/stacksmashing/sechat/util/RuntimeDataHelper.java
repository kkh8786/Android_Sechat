package net.stacksmashing.sechat.util;

import net.stacksmashing.sechat.Bus;
import net.stacksmashing.sechat.db.Contact;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by thomas on 28/12/14.
 */
public enum RuntimeDataHelper {
    INSTANCE;

    public static RuntimeDataHelper getInstance() {
        return INSTANCE;
    }

    private final Map<Long, Contact.Status> onlineStatusMap = new HashMap<>();
    private final Map<Long, Boolean> chatOpenStatus = new HashMap<>();
    private final Map<String, List<String>> groupChatUsers = new HashMap<>();

    public void setContactStatus(Contact contact, Contact.Status status) {
        onlineStatusMap.put(contact.getId(), status);
        Bus.bus().post(new Bus.OnlineStatusUpdateEvent());
    }

    public Contact.Status getContactStatus(Contact contact) {
        Contact.Status status = onlineStatusMap.get(contact.getId());
        if (status == null) {
            return Contact.Status.OFFLINE;
        }
        return status;
    }

    public void setChatIsOpen(long id, boolean value) {
        chatOpenStatus.put(id, value);
    }

    public boolean isChatOpen(long id) {
        return chatOpenStatus.get(id) != null ? chatOpenStatus.get(id) : false;
    }

    /* Temporary storage for the list of members of a group chat room that we are creating. */
    public void setGroupChatUsers(String name, List<String> users) {
        groupChatUsers.put(name, users);
    }

    public List<String> getGroupChatUsers(String name) {
        return groupChatUsers.get(name);
    }

    public void removeGroupChatUsers(String name) {
        groupChatUsers.remove(name);
    }
}
