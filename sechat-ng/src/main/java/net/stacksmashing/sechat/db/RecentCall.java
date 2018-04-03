package net.stacksmashing.sechat.db;

import android.support.annotation.DrawableRes;

import net.stacksmashing.sechat.R;

import java.util.Date;

import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_RECENT_CALLS;

@DAO.Table(name = TABLE_RECENT_CALLS)
public class RecentCall extends Entity {

    public static final DAO<RecentCall> DAO = new DAO<>(RecentCall.class);

    @DAO.Column(primaryKey = true, name = COLUMN_ID)
    long id;

    /* Call initiator (for incoming) or recipient (for outgoing). */
    @DAO.Column(notNull = true)
    String contact;

    @DAO.Column
    boolean incoming;

    @DAO.Column
    Date time;

    @DAO.Column
    long duration;

    public RecentCall() {
    }

    public RecentCall(String contact, boolean incoming, Date time, long duration) {
        this.contact = contact;
        this.incoming = incoming;
        this.time = time;
        this.duration = duration;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public String getContact() {
        return contact;
    }

    public Date getTime() {
        return time;
    }

    public long getDuration() {
        return duration;
    }

    @DrawableRes
    public int getIcon() {
        return isIncoming() ? R.drawable.ic_call_received_grey600_36dp : R.drawable.ic_call_made_grey600_36dp;
    }
}
