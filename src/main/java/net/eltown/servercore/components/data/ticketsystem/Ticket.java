package net.eltown.servercore.components.data.ticketsystem;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
public class Ticket {

    private final String creator;
    private String supporter;
    private String id;
    private String subject;
    private String section;
    private String priority;
    private Map<String, String> messages;
    private String dateOpened;
    private String dateClosed;

}
