package com.decisionhorizon;

import java.util.EnumMap;
import java.util.Map;

public class TriggerGate {
    private final Map<DecisionTrigger, Boolean> firedThisTurn = new EnumMap<>(DecisionTrigger.class);
    private final Map<DecisionTrigger, Boolean> firedThisRoom = new EnumMap<>(DecisionTrigger.class);

    public TriggerGate() {
        resetTurn();
        resetRoom();
    }

    public void resetTurn() {
        for (DecisionTrigger trigger : DecisionTrigger.values()) {
            firedThisTurn.put(trigger, false);
        }
    }

    public void resetRoom() {
        for (DecisionTrigger trigger : DecisionTrigger.values()) {
            firedThisRoom.put(trigger, false);
        }
    }

    public boolean hasFiredThisTurn(DecisionTrigger trigger) {
        return Boolean.TRUE.equals(firedThisTurn.get(trigger));
    }

    public boolean hasFiredThisRoom(DecisionTrigger trigger) {
        return Boolean.TRUE.equals(firedThisRoom.get(trigger));
    }

    public void markTurn(DecisionTrigger trigger) {
        firedThisTurn.put(trigger, true);
    }

    public void markRoom(DecisionTrigger trigger) {
        firedThisRoom.put(trigger, true);
    }
}