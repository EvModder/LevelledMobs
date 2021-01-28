package io.github.lokka30.levelledmobs.utils;

public enum Addition {

    // Prefix of ATTRIBUTE if it is a Minecraft vanilla attribute like GENERIC_MOVEMENT_SPEED
    ATTRIBUTE_MOVEMENT_SPEED("fine-tuning.additions.movement-speed"),
    ATTRIBUTE_ATTACK_DAMAGE("fine-tuning.additions.attack-damage"),
    ATTRIBUTE_MAX_HEALTH("fine-tuning.additions.max-health"),

    // Prefix of CUSTOM if it is a custom value used in listeners
    CUSTOM_RANGED_ATTACK_DAMAGE("fine-tuning.additions.ranged-attack-damage"),
    CUSTOM_ITEM_DROP("fine-tuning.additions.item-drop"),
    CUSTOM_XP_DROP("fine-tuning.additions.xp-drop");

    private final String maxAdditionConfigPath;

    Addition(String maxAdditionConfigPath) {
        this.maxAdditionConfigPath = maxAdditionConfigPath;
    }

    public String getMaxAdditionConfigPath() {
        return maxAdditionConfigPath;
    }
}
