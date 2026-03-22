package com.decisionhorizon;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostRenderSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import java.util.HashMap;
import java.util.Map;

@SpireInitializer
public class DecisionHorizonMod implements PostInitializeSubscriber, PostRenderSubscriber, PostUpdateSubscriber {

    private static boolean pauseWindowActive = false;
    private static boolean inputLocked = false;

    private static float pauseTimer = 0.0f;
    private static float pauseCooldown = 0.0f;

    private static boolean hWasDown = false;
    private static boolean spaceWasDown = false;

    private static String pauseReason = "";

    private static boolean wasInCombat = false;
    private static int lastTurnSeen = -1;
    private static int lastPlayerHp = -1;
    private static int lastThreatDamageSeen = -1;
    private static boolean threatAcknowledged = false;

    private static final Map<AbstractMonster, Integer> lastIntents = new HashMap<>();
    private static final TriggerGate triggerGate = new TriggerGate();

    private static final float DEFAULT_PAUSE_DURATION = 10.0f;
    private static final float DEFAULT_PAUSE_COOLDOWN = 0.75f;

    private static boolean audioMutedForPause = false;
    private static float savedMusicVolume = -1.0f;
    private static float savedSfxVolume = -1.0f;

    // Screen / room tracking
    private static String lastScreenName = "NONE";
    private static String lastRoomClassName = "";
    private static int lastActNumSeen = -1;

    public DecisionHorizonMod() {
        BaseMod.subscribe(this);
        System.out.println("[Decision Horizon] Constructor");
    }

    public static void initialize() {
        System.out.println("[Decision Horizon] initialize()");
        new DecisionHorizonMod();
    }

    public static boolean isInputLocked() {
        return inputLocked;
    }

    public static boolean isPauseWindowActive() {
        return pauseWindowActive;
    }

    @Override
    public void receivePostInitialize() {
        System.out.println("[Decision Horizon] PostInitialize");
    }

    @Override
    public void receivePostUpdate() {
        float dt = Gdx.graphics.getDeltaTime();

        if (pauseCooldown > 0.0f) {
            pauseCooldown -= dt;
            if (pauseCooldown < 0.0f) {
                pauseCooldown = 0.0f;
            }
        }

        if (pauseWindowActive) {
            pauseTimer -= dt;
            if (pauseTimer <= 0.0f) {
                endPause();
            }
        }

        handleDebugInput();

        detectRoomEntry();
        detectScreenTransitions();
        detectNewActMapVisible();
        detectTurnStart();
        detectIntentChanges();
        detectHpChange();
        detectThreat();
    }

    private static void handleDebugInput() {
        boolean hIsDown = Gdx.input.isKeyPressed(Input.Keys.H);
        boolean spaceIsDown = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        if (hIsDown && !hWasDown) {
            requestPause(
                    DecisionTrigger.TURN_START,
                    "Manual trigger",
                    DEFAULT_PAUSE_DURATION,
                    false,
                    false
            );
        }

        if (pauseWindowActive && spaceIsDown && !spaceWasDown) {
            endPause();
        }

        hWasDown = hIsDown;
        spaceWasDown = spaceIsDown;
    }

    private static void endPause() {
        pauseWindowActive = false;
        inputLocked = false;
        pauseTimer = 0.0f;
        restoreAudioAfterPause();
        System.out.println("[Decision Horizon] Pause ended");
    }

    public static void requestPause(
            DecisionTrigger trigger,
            String reason,
            float duration,
            boolean oncePerTurn,
            boolean oncePerRoom
    ) {
        if (pauseWindowActive || pauseCooldown > 0.0f) {
            return;
        }

        // Prevent duplicate identical reasons from firing back-to-back
        if (pauseReason != null && pauseReason.equals(reason)) {
            return;
        }

        if (oncePerTurn && triggerGate.hasFiredThisTurn(trigger)) {
            return;
        }

        if (oncePerRoom && triggerGate.hasFiredThisRoom(trigger)) {
            return;
        }

        if (oncePerTurn) {
            triggerGate.markTurn(trigger);
        }

        if (oncePerRoom) {
            triggerGate.markRoom(trigger);
        }

        pauseWindowActive = true;
        inputLocked = true;
        pauseTimer = duration;
        pauseReason = reason;
        pauseCooldown = DEFAULT_PAUSE_COOLDOWN;

        muteAudioForPause();

        System.out.println("[Decision Horizon] Pause triggered: " + trigger + " -> " + reason);
    }

    private static void muteAudioForPause() {
        if (audioMutedForPause) {
            return;
        }

        savedMusicVolume = Settings.MUSIC_VOLUME;
        savedSfxVolume = Settings.SOUND_VOLUME;

        Settings.MUSIC_VOLUME = 0.0f;
        Settings.SOUND_VOLUME = 0.0f;

        audioMutedForPause = true;
    }

    private static void restoreAudioAfterPause() {
        if (!audioMutedForPause) {
            return;
        }

        if (savedMusicVolume >= 0.0f) {
            Settings.MUSIC_VOLUME = savedMusicVolume;
        }

        if (savedSfxVolume >= 0.0f) {
            Settings.SOUND_VOLUME = savedSfxVolume;
        }

        audioMutedForPause = false;
    }

    private static void detectRoomEntry() {
        AbstractRoom room = getCurrentRoomSafe();
        if (room == null) {
            return;
        }

        String roomClassName = room.getClass().getSimpleName();

        if (!roomClassName.equals(lastRoomClassName)) {
            lastRoomClassName = roomClassName;
            triggerGate.resetRoom();

            // Per your friend’s note: skip enter-node pauses for event rooms for now.
            if (!roomClassName.contains("EventRoom")) {
                requestPause(
                        DecisionTrigger.ENTER_NODE,
                        "Entered node: " + roomClassName,
                        DEFAULT_PAUSE_DURATION,
                        false,
                        true
                );
            }
        }
    }

    private static void detectScreenTransitions() {
        String currentScreenName = getCurrentScreenName();

        if (!currentScreenName.equals(lastScreenName)) {
            lastScreenName = currentScreenName;

            if ("COMBAT_REWARD".equals(currentScreenName)) {
                requestPause(
                        DecisionTrigger.REWARD_SCREEN_OPENED,
                        "Reward screen opened",
                        DEFAULT_PAUSE_DURATION,
                        false,
                        true
                );
            } else if ("CARD_REWARD".equals(currentScreenName)) {
                requestPause(
                        DecisionTrigger.CARD_REWARD_OPENED,
                        "Card reward opened",
                        DEFAULT_PAUSE_DURATION,
                        false,
                        true
                );
            } else if ("SHOP".equals(currentScreenName)) {
                requestPause(
                        DecisionTrigger.ENTER_NODE,
                        "Shop opened",
                        DEFAULT_PAUSE_DURATION,
                        false,
                        true
                );
            }
        }
    }

    private static void detectNewActMapVisible() {
        if (AbstractDungeon.actNum != lastActNumSeen) {
            lastActNumSeen = AbstractDungeon.actNum;

            if ("MAP".equals(getCurrentScreenName())) {
                requestPause(
                        DecisionTrigger.NEW_ACT_MAP_VISIBLE,
                        "New Act map visible",
                        DEFAULT_PAUSE_DURATION,
                        false,
                        true
                );
            }
        }
    }

    private static void detectTurnStart() {
        if (!isInCombat()) {
            resetCombatState();
            return;
        }

        if (!wasInCombat) {
            wasInCombat = true;
            lastTurnSeen = -1;
            lastPlayerHp = AbstractDungeon.player != null ? AbstractDungeon.player.currentHealth : -1;
            lastIntents.clear();
            triggerGate.resetTurn();
        }

        if (AbstractDungeon.actionManager == null) {
            return;
        }

        int turn = AbstractDungeon.actionManager.turn;

        if (turn > 0 && turn != lastTurnSeen) {
            lastTurnSeen = turn;
            triggerGate.resetTurn();

            requestPause(
                    DecisionTrigger.TURN_START,
                    "Turn " + turn + " started",
                    DEFAULT_PAUSE_DURATION,
                    true,
                    false
            );
        }
    }

    private static void detectIntentChanges() {
        if (!isInCombat()) {
            return;
        }
        if (AbstractDungeon.getMonsters() == null || AbstractDungeon.getMonsters().monsters == null) {
            return;
        }

        boolean changed = false;

        for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
            if (monster == null || monster.isDeadOrEscaped()) {
                continue;
            }

            int intentOrdinal = monster.intent.ordinal();

            if (!lastIntents.containsKey(monster)) {
                lastIntents.put(monster, intentOrdinal);
            } else if (lastIntents.get(monster) != intentOrdinal) {
                changed = true;
                lastIntents.put(monster, intentOrdinal);
            }
        }

        if (changed) {
            requestPause(
                    DecisionTrigger.ENEMY_INTENT_CHANGED,
                    "Enemy intent changed",
                    DEFAULT_PAUSE_DURATION,
                    true,
                    false
            );
        }
    }

    private static void detectHpChange() {
        if (!isInCombat() || AbstractDungeon.player == null) {
            return;
        }

        int hp = AbstractDungeon.player.currentHealth;

        if (lastPlayerHp < 0) {
            lastPlayerHp = hp;
            return;
        }

        if (hp != lastPlayerHp) {
            int delta = hp - lastPlayerHp;
            lastPlayerHp = hp;

            if (delta < 0) {
                requestPause(
                        DecisionTrigger.TURN_START,
                        "Took damage: " + (-delta),
                        DEFAULT_PAUSE_DURATION,
                        false,
                        false
                );
            }
        }
    }

    private static void detectThreat() {
        if (!isInCombat() || AbstractDungeon.player == null) {
            lastThreatDamageSeen = -1;
            threatAcknowledged = false;
            return;
        }

        if (AbstractDungeon.getMonsters() == null || AbstractDungeon.getMonsters().monsters == null) {
            return;
        }

        int totalIntentDamage = 0;

        for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
            if (monster == null || monster.isDeadOrEscaped()) {
                continue;
            }

            int dmg = monster.getIntentBaseDmg();
            if (dmg > 0) {
                totalIntentDamage += dmg;
            }
        }

        int block = AbstractDungeon.player.currentBlock;
        int hp = AbstractDungeon.player.currentHealth;
        int incomingAfterBlock = Math.max(0, totalIntentDamage - block);

        if (incomingAfterBlock != lastThreatDamageSeen) {
            lastThreatDamageSeen = incomingAfterBlock;
            threatAcknowledged = false;
        }

        if (!threatAcknowledged) {
            if (incomingAfterBlock >= hp && totalIntentDamage > 0) {
                threatAcknowledged = true;
                requestPause(
                        DecisionTrigger.ENEMY_INTENT_CHANGED,
                        "LETHAL incoming",
                        DEFAULT_PAUSE_DURATION,
                        false,
                        false
                );
            } else if (totalIntentDamage >= 15) {
                threatAcknowledged = true;
                requestPause(
                        DecisionTrigger.ENEMY_INTENT_CHANGED,
                        "High damage incoming: " + totalIntentDamage,
                        DEFAULT_PAUSE_DURATION,
                        false,
                        false
                );
            }
        }
    }

    private static void resetCombatState() {
        wasInCombat = false;
        lastTurnSeen = -1;
        lastPlayerHp = -1;
        lastIntents.clear();
        lastThreatDamageSeen = -1;
        threatAcknowledged = false;
        triggerGate.resetTurn();
    }

    private static boolean isInCombat() {
        if (AbstractDungeon.player == null) {
            return false;
        }
        if (AbstractDungeon.actionManager == null) {
            return false;
        }

        AbstractRoom room = getCurrentRoomSafe();
        return room != null && room.phase == AbstractRoom.RoomPhase.COMBAT;
    }

    private static AbstractRoom getCurrentRoomSafe() {
        try {
            return AbstractDungeon.getCurrRoom();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getCurrentScreenName() {
        try {
            return AbstractDungeon.screen == null ? "NONE" : AbstractDungeon.screen.name();
        } catch (Exception e) {
            return "NONE";
        }
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        if (!pauseWindowActive) {
            return;
        }

        FontHelper.renderFontCentered(
                sb,
                FontHelper.panelNameFont,
                "=== DECISION HORIZON ACTIVE ===",
                Settings.WIDTH / 2.0f,
                Settings.HEIGHT / 2.0f + 140.0f,
                Color.RED
        );

        FontHelper.renderFontCentered(
                sb,
                FontHelper.cardTitleFont,
                pauseReason,
                Settings.WIDTH / 2.0f,
                Settings.HEIGHT / 2.0f + 60.0f,
                Color.WHITE
        );

        FontHelper.renderFontCentered(
                sb,
                FontHelper.cardTitleFont,
                "TIME LEFT: " + String.format("%.1f", pauseTimer),
                Settings.WIDTH / 2.0f,
                Settings.HEIGHT / 2.0f,
                Color.GOLD
        );

        FontHelper.renderFontCentered(
                sb,
                FontHelper.topPanelInfoFont,
                "INPUT LOCKED",
                Settings.WIDTH / 2.0f,
                Settings.HEIGHT / 2.0f - 80.0f,
                Color.CYAN
        );

        FontHelper.renderFontCentered(
                sb,
                FontHelper.topPanelInfoFont,
                "SPACE = skip",
                Settings.WIDTH / 2.0f,
                Settings.HEIGHT / 2.0f - 120.0f,
                Color.LIGHT_GRAY
        );
    }
}