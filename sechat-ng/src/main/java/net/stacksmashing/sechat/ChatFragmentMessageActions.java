package net.stacksmashing.sechat;

import net.stacksmashing.sechat.db.Message;

/**
 * Created by kulikov on 16.12.2014.
 */
public interface ChatFragmentMessageActions {

    void viewLocation(Message message);

    void viewVideo(Message message);

    void viewImage(Message message);

    void viewContact(Message message);
}
