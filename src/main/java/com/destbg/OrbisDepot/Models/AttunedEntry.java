package com.destbg.OrbisDepot.Models;

import java.util.UUID;

public record AttunedEntry(UUID ownerUUID, String ownerName) {
    @Override
    public boolean equals(Object o) {
        return o instanceof AttunedEntry e && ownerUUID.equals(e.ownerUUID);
    }

    @Override
    public int hashCode() {
        return ownerUUID.hashCode();
    }
}
