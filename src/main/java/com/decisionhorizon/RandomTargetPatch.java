package com.decisionhorizon.patches;

import com.decisionhorizon.DecisionHorizonMod;
import com.decisionhorizon.DecisionTrigger;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

@SpirePatch(
        clz = UseCardAction.class,
        method = "update"
)
public class RandomTargetPatch {

    public static void Prefix(UseCardAction __instance) {
        AbstractCard card = __instance.targetCard;

        if (card == null) return;

        // Only care if multiple enemies exist
        if (AbstractDungeon.getMonsters() == null ||
            AbstractDungeon.getMonsters().monsters == null ||
            AbstractDungeon.getMonsters().monsters.size() <= 1) {
            return;
        }

        // Detect random targeting
        if (card.target == AbstractCard.CardTarget.ALL_ENEMY ||
            card.target == AbstractCard.CardTarget.RANDOM_ENEMY) {

            DecisionHorizonMod.requestPause(
                    DecisionTrigger.RANDOM_TARGET_CARD_PLAYED,
                    "Random target: " + card.name,
                    8.0f,
                    false,
                    false
            );
        }
    }
}