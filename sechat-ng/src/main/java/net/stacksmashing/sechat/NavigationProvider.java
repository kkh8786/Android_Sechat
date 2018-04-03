package net.stacksmashing.sechat;

import net.stacksmashing.sechat.db.Contact;

public interface NavigationProvider {
    void openChatById(long id);

    void openChatForContact(Contact contact, boolean secret);
}
