package net.stacksmashing.sechat.db;

import java.io.Serializable;

import static net.stacksmashing.sechat.db.DatabaseHelper.COLUMN_ID;
import static net.stacksmashing.sechat.db.DatabaseHelper.TABLE_DOWNLOADS;

@DAO.Table(name = TABLE_DOWNLOADS)
public class Download extends Entity implements Serializable {
    public static final DAO<Download> DAO = new DAO<>(Download.class);

    @DAO.Column(name = COLUMN_ID, primaryKey = true)
    long id;

    @DAO.Column(notNull = true, unique = true)
    String uuid;

    @DAO.Column
    int numParts;

    @DAO.Column
    int lastReceivedPart;

    @DAO.Column
    int type;

    public Download() {
    }

    public Download(String uuid, int type, int numParts) {
        this.uuid = uuid;
        this.type = type;
        this.numParts = numParts;
        this.lastReceivedPart = -1;
    }

    public long getId() {
        return id;
    }

    public void setLastReceivedPart(int lastReceivedPart) {
        this.lastReceivedPart = lastReceivedPart;
    }

    public boolean isComplete() {
        return getReceivedParts() == getTotalParts();
    }

    public boolean isNextPartNumber(int partNum) {
        return !isComplete() && lastReceivedPart < partNum;
    }

    public int getTotalParts() {
        return numParts;
    }

    /* lastReceivedPart is an index, so add 1 to make it the count. */
    public int getReceivedParts() {
        return lastReceivedPart + 1;
    }
}
