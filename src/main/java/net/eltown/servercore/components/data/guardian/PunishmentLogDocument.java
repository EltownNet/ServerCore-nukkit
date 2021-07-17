package net.eltown.servercore.components.data.guardian;

import lombok.Data;

@Data
public class PunishmentLogDocument {

    private final String id;
    private final String logId;
    private final String target;
    private final String reason;
    private final String executor;
    private final String date;
    private final long timeEnd;
    private final long timeStart;
}
