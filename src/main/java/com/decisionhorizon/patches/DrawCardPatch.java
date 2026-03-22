package com.decisionhorizon.patches;

import com.decisionhorizon.DecisionHorizonMod;
import com.decisionhorizon.DecisionTrigger;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

@SpirePatch(
        clz = DrawCardAction.class,
        method = "update"
)
public class DrawCardPatch {

    private static int lastSeenHandSize = -1;
    private static int lastSeenTurn = -1;

    public static void Postfix(DrawCardAction __instance) {
        if (AbstractDungeon.player == null || AbstractDungeon.actionManager == null) {
            return;
        }

        int currentTurn = AbstractDungeon.actionManager.turn;
        int currentHandSize = AbstractDungeon.player.hand != null
                ? AbstractDungeon.player.hand.size()
                : -1;

        if (currentTurn != lastSeenTurn) {
            lastSeenTurn = currentTurn;
            lastSeenHandSize = currentHandSize;
            return;
        }

        if (currentHandSize > lastSeenHandSize) {
            lastSeenHandSize = currentHandSize;

            DecisionHorizonMod.requestPause(
                    DecisionTrigger.DRAW_CARDS,
                    "Cards drawn",
                    6.0f,
                    true,
                    false
            );
        } else {
            lastSeenHandSize = currentHandSize;
        }
    }
}