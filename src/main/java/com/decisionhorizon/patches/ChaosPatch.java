package com.decisionhorizon.patches;

import com.decisionhorizon.DecisionHorizonMod;
import com.decisionhorizon.DecisionTrigger;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.unique.ChaosAction;

@SpirePatch(
        clz = ChaosAction.class,
        method = "update"
)
public class ChaosPatch {

    public static void Postfix(ChaosAction __instance) {
        DecisionHorizonMod.requestPause(
                DecisionTrigger.CHAOS_PLAYED,
                "Chaos generated random cards",
                8.0f,
                false,
                false
        );
    }
}