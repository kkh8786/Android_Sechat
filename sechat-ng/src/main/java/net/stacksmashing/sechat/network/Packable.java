package net.stacksmashing.sechat.network;

import java.util.Map;

interface Packable {
    void pack(Map<String, Object> values);
}
