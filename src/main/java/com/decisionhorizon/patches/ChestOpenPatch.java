package com.decisionhorizon.patches;

import com.decisionhorizon.DecisionHorizonMod;
import com.decisionhorizon.DecisionTrigger;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.rooms.TreasureRoom;

@SpirePatch(
        clz = TreasureRoom.class,
        method = "openChest"
)
public class ChestOpenPatch {

    public static void Postfix(TreasureRoom __instance) {
        DecisionHorizonMod.requestPause(
                DecisionTrigger.CHEST_OPENED,
                "Chest opened",
                10.0f,
                false,
                true
        );
    }
}