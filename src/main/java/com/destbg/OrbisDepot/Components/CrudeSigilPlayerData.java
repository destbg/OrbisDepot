package com.destbg.OrbisDepot.Components;

import com.destbg.OrbisDepot.Utils.ComponentUtils;
import com.destbg.OrbisDepot.Utils.Constants;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public class CrudeSigilPlayerData implements Component<EntityStore> {

    public static final BuilderCodec<CrudeSigilPlayerData> CODEC = BuilderCodec.builder(CrudeSigilPlayerData.class, CrudeSigilPlayerData::new)
            .append(new KeyedCodec<>("ItemContainer", SimpleItemContainer.CODEC),
                    (state, container) -> {
                        if (container != null) {
                            state.itemContainer = container;
                        }
                    },
                    s -> s.itemContainer).add()
            .append(new KeyedCodec<>("ElapsedTime", Codec.FLOAT),
                    (component, value) -> component.elapsedTime = value,
                    component -> component.elapsedTime).add()
            .append(new KeyedCodec<>("SelectedAttunement", Codec.UUID_BINARY),
                    (component, value) -> component.selectedAttunement = value,
                    component -> component.selectedAttunement).add()
            .build();

    private SimpleItemContainer itemContainer;
    private UUID selectedAttunement;
    private float elapsedTime;

    public CrudeSigilPlayerData() {
        itemContainer = new SimpleItemContainer(Constants.CRUDE_SIGIL_UPLOAD_SLOT_CAPACITY);
    }

    public CrudeSigilPlayerData(CrudeSigilPlayerData clone) {
        this.itemContainer = clone.itemContainer;
        this.elapsedTime = clone.elapsedTime;
    }

    public SimpleItemContainer getItemContainer() {
        return itemContainer;
    }

    public UUID getSelectedAttunement() {
        return selectedAttunement;
    }

    public void setSelectedAttunement(UUID selectedAttunement) {
        this.selectedAttunement = selectedAttunement;
    }

    public float getTickInterval() {
        return Constants.UPLOAD_INTERVAL_CRUDE_SIGIL_SECONDS;
    }

    public float getElapsedTime() {
        return elapsedTime;
    }

    public void addElapsedTime(float dt) {
        this.elapsedTime += dt;
    }

    public void resetElapsedTime() {
        this.elapsedTime = 0f;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new CrudeSigilPlayerData(this);
    }

    public static ComponentType<EntityStore, CrudeSigilPlayerData> getComponentType() {
        return ComponentUtils.getCrudeSigilPlayerDataComponent();
    }
}
