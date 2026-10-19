package dev.minebit.link.model;

import java.util.List;

public record Snapshot(String networkName, String networkUrl, List<Role> roles) {

    public static final Snapshot EMPTY = new Snapshot("", "", List.of());

}
