package com.decisionhorizon.patches;

import com.decisionhorizon.DecisionHorizonMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class CardUsePatch {

    @SpirePatch(
            clz = AbstractPlayer.class,
            method = "useCard",
            paramtypez = {
                    AbstractCard.class,
                    AbstractMonster.class,
                    int.class
            }
    )
    public static class BlockCardPlay {
        public static SpireReturn<Void> Prefix(
                AbstractPlayer __instance,
                AbstractCard c,
                AbstractMonster monster,
                int energyOnUse
        ) {
            if (DecisionHorizonMod.isInputLocked()) {
                System.out.println("[Decision Horizon] Blocked card play: " + c.name);
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }
    }
}