package com.safari.block;

import com.safari.SafariMod;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class SafariBlocks {
    public static final Block SAFARI_PORTAL_FRAME = registerBlock(
            "safari_portal_frame",
            new Block(AbstractBlock.Settings.copy(Blocks.OBSIDIAN))
    );

    public static final Block SAFARI_PORTAL = registerBlock(
            "safari_portal",
            new SafariPortalBlock(AbstractBlock.Settings.copy(Blocks.NETHER_PORTAL)
                    .noCollision()
                    .strength(-1.0F, 3600000.0F)
                    .luminance(state -> 11)
                    .dropsNothing())
    );

    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(SafariMod.MOD_ID, name), new BlockItem(block, new Item.Settings()));
        return Registry.register(Registries.BLOCK, Identifier.of(SafariMod.MOD_ID, name), block);
    }

    public static void registerModBlocks() {
        SafariMod.LOGGER.info("Registering Mod Blocks for " + SafariMod.MOD_ID);
    }
}
