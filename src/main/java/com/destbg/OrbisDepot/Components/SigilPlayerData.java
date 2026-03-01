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

public class SigilPlayerData implements Component<EntityStore> {

    public static final BuilderCodec<SigilPlayerData> CODEC = BuilderCodec.builder(SigilPlayerData.class, SigilPlayerData::new)
            .append(new KeyedCodec<>("ItemContainer", SimpleItemContainer.CODEC),
                    (state, container) -> {
                        if (container != null) {
                            state.itemContainer = container;
                        }
                    },
                    s -> s.itemContainer).add()
            .append(new KeyedCodec<>("SelectedAttunement", Codec.UUID_BINARY),
                    (component, value) -> component.selectedAttunement = value,
                    component -> component.selectedAttunement).add()
            .append(new KeyedCodec<>("AutoRestore", Codec.BOOLEAN),
                    (component, value) -> component.autoRestore = value,
                    component -> component.autoRestore).add()
            .append(new KeyedCodec<>("CraftingIntegration", Codec.BOOLEAN),
                    (component, value) -> component.craftingIntegration = value,
                    component -> component.craftingIntegration).add()
            .append(new KeyedCodec<>("ThrottleUiUpdates", Codec.BOOLEAN),
                    (component, value) -> component.throttleUiUpdates = value,
                    component -> component.throttleUiUpdates).add()
            .build();

    private SimpleItemContainer itemContainer;
    private UUID selectedAttunement;
    private boolean autoRestore;
    private boolean craftingIntegration;
    private boolean throttleUiUpdates;

    public SigilPlayerData() {
        itemContainer = new SimpleItemContainer(Constants.SIGIL_UPLOAD_SLOT_CAPACITY);
        autoRestore = true;
        craftingIntegration = true;
        throttleUiUpdates = false;
    }

    public SigilPlayerData(SigilPlayerData clone) {
        this.itemContainer = clone.itemContainer;
        this.selectedAttunement = clone.selectedAttunement;
        this.autoRestore = clone.autoRestore;
        this.craftingIntegration = clone.craftingIntegration;
        this.throttleUiUpdates = clone.throttleUiUpdates;
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

    public boolean getAutoRestore() {
        return autoRestore;
    }

    public void setAutoRestore(boolean autoRestore) {
        this.autoRestore = autoRestore;
    }

    public boolean setCraftingIntegration() {
        return craftingIntegration;
    }

    public void setCraftingIntegration(boolean craftingIntegration) {
        this.craftingIntegration = craftingIntegration;
    }

    public boolean isThrottleUiUpdates() {
        return throttleUiUpdates;
    }

    public void setThrottleUiUpdates(boolean throttleUiUpdates) {
        this.throttleUiUpdates = throttleUiUpdates;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new SigilPlayerData(this);
    }

    public static ComponentType<EntityStore, SigilPlayerData> getComponentType() {
        return ComponentUtils.getSigilPlayerDataComponent();
    }
}
