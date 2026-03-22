package com.decisionhorizon.patches;

import com.decisionhorizon.DecisionHorizonMod;
import com.decisionhorizon.DecisionTrigger;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.unique.DiscoveryAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

@SpirePatch(
        clz = DiscoveryAction.class,
        method = "update"
)
public class DiscoveryActionPatch {

    private static boolean wasCardRewardScreen = false;

    public static void Postfix(DiscoveryAction __instance) {
        String screenName = AbstractDungeon.screen == null ? "NONE" : AbstractDungeon.screen.name();
        boolean isCardRewardScreen = "CARD_REWARD".equals(screenName);

        if (isCardRewardScreen && !wasCardRewardScreen) {
            DecisionHorizonMod.requestPause(
                    DecisionTrigger.CARD_GENERATION_OPTIONS,
                    "Random card options",
                    10.0f,
                    false,
                    false
            );
        }

        wasCardRewardScreen = isCardRewardScreen;
    }
}