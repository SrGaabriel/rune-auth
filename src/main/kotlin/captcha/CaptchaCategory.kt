package com.runerealms.auth.captcha

import org.bukkit.Material

data class CaptchaCategory(
    val name: String,
    val materials: List<Material>
)

object DefaultCaptchaCategories {
    val Combat = CaptchaCategory(
        name = "Combate",
        materials = listOf(
            Material.DIAMOND_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.STONE_SWORD,
            Material.WOODEN_SWORD,
            Material.BOW,
            Material.CROSSBOW,
            Material.TRIDENT,
            Material.SHIELD,
            Material.TOTEM_OF_UNDYING
        )
    )

    val Mining = CaptchaCategory(
        name = "Mineração",
        materials = listOf(
            Material.DIAMOND_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.WOODEN_PICKAXE,
            Material.DIAMOND_SHOVEL
        )
    )

    val Farming = CaptchaCategory(
        name = "Fazenda",
        materials = listOf(
            Material.WHEAT,
            Material.BEETROOT,
            Material.CARROT,
            Material.POTATO,
            Material.MELON,
            Material.PUMPKIN,
            Material.SUGAR_CANE,
            Material.COCOA_BEANS,
            Material.BAMBOO,
        )
    )

    val Exploration = CaptchaCategory(
        name = "Exploração",
        materials = listOf(
            Material.COMPASS,
            Material.MAP,
            Material.CLOCK,
            Material.SPYGLASS,
            Material.ELYTRA,
        )
    )

    val Redstone = CaptchaCategory(
        name = "Redstone",
        materials = listOf(
            Material.REDSTONE,
            Material.REDSTONE_TORCH,
            Material.REDSTONE_BLOCK,
            Material.REPEATER,
            Material.COMPARATOR,
            Material.OBSERVER,
            Material.DISPENSER,
            Material.DROPPER,
        )
    )

    val Building = CaptchaCategory(
        name = "Construção",
        materials = listOf(
            Material.BRICK,
            Material.BRICKS,
            Material.BRICK_STAIRS,
            Material.BRICK_SLAB,
            Material.BRICK_WALL,
            Material.STONE,
            Material.STONE_SLAB,
            Material.STONE_STAIRS,
            Material.STONE_BRICKS,
            Material.STONE_BRICK_SLAB,
            Material.STONE_BRICK_STAIRS,
            Material.STONE_BRICK_WALL
        )
    )

    val Brewing = CaptchaCategory(
        name = "Alquimia",
        materials = listOf(
            Material.BREWING_STAND,
            Material.CAULDRON,
            Material.GLASS_BOTTLE,
            Material.POTION,
            Material.LINGERING_POTION,
            Material.SPLASH_POTION,
            Material.DRAGON_BREATH
        )
    )

    val Storage = CaptchaCategory(
        name = "Armazenamento",
        materials = listOf(
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.ENDER_CHEST,
            Material.SHULKER_BOX,
            Material.WHITE_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX
        )
    )

    val Transportation = CaptchaCategory(
        name = "Transporte",
        materials = listOf(
            Material.MINECART,
            Material.POWERED_RAIL,
            Material.DETECTOR_RAIL,
            Material.RAIL,
            Material.ACTIVATOR_RAIL,
            Material.OAK_BOAT,
            Material.SADDLE
        )
    )

    val All: List<CaptchaCategory> = listOf(
        Combat,
        Mining,
        Farming,
        Exploration,
        Redstone,
        Building,
        Brewing,
        Transportation
    )
}