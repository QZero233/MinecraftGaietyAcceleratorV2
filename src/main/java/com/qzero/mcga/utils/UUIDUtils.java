package com.qzero.mcga.utils;

import java.util.UUID;

public class UUIDUtils {
    public static String getRandomUUID(){
        UUID uuid=UUID.randomUUID();
        return uuid.toString();
    }
}
