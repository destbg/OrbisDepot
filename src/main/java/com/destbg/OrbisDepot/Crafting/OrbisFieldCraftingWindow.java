package com.destbg.OrbisDepot.Crafting;

import com.destbg.OrbisDepot.Utils.CraftingUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.builtin.crafting.window.FieldCraftingWindow;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class OrbisFieldCraftingWindow extends FieldCraftingWindow {

    @Override
    public boolean onOpen0(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
        boolean result = super.onOpen0(ref, store);
        if (result) {
            CraftingUtils.injectForPlayer(ref, store, this);
        }
        return result;
    }
}
