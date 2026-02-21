package com.destbg.OrbisDepot.Models;

import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class StorageModel {

    public static final BuilderCodec<StorageModel> CODEC = BuilderCodec.builder(StorageModel.class, StorageModel::new)
            .append(new KeyedCodec<>(Constants.KEY_ACTION, Codec.STRING), (m, s) -> m.action = s, m -> m.action).add()
            .append(new KeyedCodec<>(Constants.KEY_SEARCH_QUERY, Codec.STRING), (m, s) -> m.searchQuery = s, m -> m.searchQuery).add()
            .append(new KeyedCodec<>(Constants.KEY_SLOT_INDEX, Codec.INTEGER), (m, i) -> m.slotIndex = i != null ? i : -1, m -> m.slotIndex).add()
            .append(new KeyedCodec<>(Constants.KEY_CHECKBOX, Codec.STRING), (m, s) -> m.checkbox = s, m -> m.checkbox).add()
            .build();

    private String action;
    private String searchQuery;
    private int slotIndex = -1;
    private String checkbox;

    public String getAction() {
        return action;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public String getCheckbox() {
        return checkbox;
    }
}
