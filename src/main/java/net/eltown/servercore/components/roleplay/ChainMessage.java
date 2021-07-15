package net.eltown.servercore.components.roleplay;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ChainMessage {

    private final String message;
    private final int seconds;

}
