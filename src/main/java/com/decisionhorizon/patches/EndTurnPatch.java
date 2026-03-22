package com.decisionhorizon.patches;

import com.decisionhorizon.DecisionHorizonMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;

public class EndTurnPatch {

    @SpirePatch(
            clz = EndTurnButton.class,
            method = "update"
    )
    public static class BlockEndTurnUpdate {
        public static SpireReturn<Void> Prefix(EndTurnButton __instance) {
            if (DecisionHorizonMod.isInputLocked()) {
                System.out.println("[Decision Horizon] Blocked end turn during pause");
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }
    }
}