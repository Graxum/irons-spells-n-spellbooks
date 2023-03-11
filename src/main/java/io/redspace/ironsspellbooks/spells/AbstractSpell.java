package io.redspace.ironsspellbooks.spells;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerMagicData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.network.ClientboundCastError;
import io.redspace.ironsspellbooks.network.ClientboundSyncMana;
import io.redspace.ironsspellbooks.network.ClientboundUpdateCastingState;
import io.redspace.ironsspellbooks.network.spell.ClientboundOnCastStarted;
import io.redspace.ironsspellbooks.network.spell.ClientboundOnClientCast;
import io.redspace.ironsspellbooks.player.ClientInputEvents;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import io.redspace.ironsspellbooks.registries.AttributeRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.setup.Messages;
import io.redspace.ironsspellbooks.util.Utils;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractSpell {
    public static ResourceLocation ANIMATION_RESOURCE = new ResourceLocation(IronsSpellbooks.MODID, "animation");
    public static ResourceLocation ANIMATION_INSTANT_CAST = new ResourceLocation(IronsSpellbooks.MODID, "instant_projectile");
    public static ResourceLocation ANIMATION_CONTINUOUS_CAST = new ResourceLocation(IronsSpellbooks.MODID, "continuous_thrust");

    private final SpellType spellType;
    private final CastType castType;
    protected int level;
    protected int baseManaCost;
    protected int manaCostPerLevel;
    protected int baseSpellPower;
    protected int spellPowerPerLevel;
    //All time values in ticks
    protected int castTime;
    //protected int cooldown;

    private final LazyOptional<Double> manaMultiplier;
    private final LazyOptional<Double> powerMultiplier;
    private final LazyOptional<Integer> cooldown;


    protected final List<MutableComponent> uniqueInfo = new ArrayList<>();

    public AbstractSpell(SpellType spellType) {
        this.spellType = spellType;
        this.castType = spellType.getCastType();

        manaMultiplier = LazyOptional.of(() -> (ServerConfigs.getSpellConfig(spellType).MANA_MULTIPLIER));
        powerMultiplier = LazyOptional.of(() -> (ServerConfigs.getSpellConfig(spellType).POWER_MULTIPLIER));
        cooldown = LazyOptional.of(() -> ((int) (ServerConfigs.getSpellConfig(spellType).COOLDOWN_IN_SECONDS * 20)));


    }

    public int getID() {
        return this.spellType.getValue();
    }

    public SpellType getSpellType() {
        return this.spellType;
    }

    public SpellRarity getRarity() {
        return spellType.getRarity(level);
    }

    public CastType getCastType() {
        return this.castType;
    }

    public SchoolType getSchoolType() {
        return spellType.getSchoolType();
    }

    public DamageSource getDamageSource() {
        return this.spellType.getDamageSource();
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getManaCost() {
        return (int) ((baseManaCost + manaCostPerLevel * (level - 1)) * manaMultiplier.orElse(1d));
    }

    public int getSpellCooldown() {
        return this.cooldown.orElse(200);
    }

    public int getCastTime() {
        return this.castTime;
    }

    public abstract Optional<SoundEvent> getCastStartSound();

    /**
     * Default Animations Based on Cast Type. Override for specific spell-based animations
    */
    public ResourceLocation getCastAnimation(Player player) {
        return switch (this.castType){
            case INSTANT -> ANIMATION_INSTANT_CAST;
            case CONTINUOUS -> ANIMATION_CONTINUOUS_CAST;
            default -> null;
        };
    }

    public Optional<KeyframeAnimation> keyFrameAnimationOf(ResourceLocation animation){
        return Optional.of(PlayerAnimationRegistry.getAnimation(animation));
    }

    public abstract Optional<SoundEvent> getCastFinishSound();

    public float getSpellPower(Entity sourceEntity) {
        float entitySpellPowerModifier = 1;
        float entitySchoolPowerModifier = 1;
        float configPowerModifier = (powerMultiplier.orElse(1d)).floatValue();
        if (sourceEntity instanceof LivingEntity sourceLivingEntity) {
            entitySpellPowerModifier = (float) sourceLivingEntity.getAttributeValue(AttributeRegistry.SPELL_POWER.get());
            switch (this.getSchoolType()) {
                case FIRE -> entitySchoolPowerModifier = (float) sourceLivingEntity.getAttributeValue(AttributeRegistry.FIRE_SPELL_POWER.get());
                case ICE -> entitySchoolPowerModifier = (float) sourceLivingEntity.getAttributeValue(AttributeRegistry.ICE_SPELL_POWER.get());
                case LIGHTNING -> entitySchoolPowerModifier = (float) sourceLivingEntity.getAttributeValue(AttributeRegistry.LIGHTNING_SPELL_POWER.get());
                case HOLY -> entitySchoolPowerModifier = (float) sourceLivingEntity.getAttributeValue(AttributeRegistry.HOLY_SPELL_POWER.get());
                case ENDER -> entitySchoolPowerModifier = (float) sourceLivingEntity.getAttributeValue(AttributeRegistry.ENDER_SPELL_POWER.get());
                case BLOOD -> entitySchoolPowerModifier = (float) sourceLivingEntity.getAttributeValue(AttributeRegistry.BLOOD_SPELL_POWER.get());
                case EVOCATION -> entitySchoolPowerModifier = (float) sourceLivingEntity.getAttributeValue(AttributeRegistry.EVOCATION_SPELL_POWER.get());
            }
        }

        return (baseSpellPower + spellPowerPerLevel * (level - 1)) * entitySpellPowerModifier * entitySchoolPowerModifier * configPowerModifier;
    }

    public int getEffectiveCastTime(Entity sourceEntity) {
        float entityCastTimeModifer = 1;
        if (sourceEntity instanceof LivingEntity sourceLivingEntity) {
            entityCastTimeModifer = 2 - (float) sourceLivingEntity.getAttributeValue(AttributeRegistry.CAST_TIME_REDUCTION.get());
        }

        return Math.round(this.castTime * entityCastTimeModifer);
    }

    public static AbstractSpell getSpell(SpellType spellType, int level) {
        return spellType.getSpellForType(level);
    }

    public static AbstractSpell getSpell(int spellId, int level) {
        return getSpell(SpellType.values()[spellId], level);
    }

    /**
     * returns true/false for success/failure to cast
     */
    public boolean attemptInitiateCast(ItemStack stack, Level level, Player player, CastSource castSource, boolean triggerCooldown) {
        if (level.isClientSide) {
            return false;
        }

        var serverPlayer = (ServerPlayer) player;
        var playerMagicData = PlayerMagicData.getPlayerMagicData(serverPlayer);

        if (!playerMagicData.isCasting()) {
            int playerMana = playerMagicData.getMana();

            boolean hasEnoughMana = playerMana - getManaCost() >= 0;
            boolean isSpellOnCooldown = playerMagicData.getPlayerCooldowns().isOnCooldown(spellType);

            if ((castSource == CastSource.SPELLBOOK || castSource == CastSource.SWORD) && isSpellOnCooldown) {
                Messages.sendToPlayer(new ClientboundCastError(ClientboundCastError.CastErrorMessages.COOLDOWN.id, this.spellType.getValue()), serverPlayer);
                return false;
            }

            if (castSource.consumesMana() && !hasEnoughMana) {
                Messages.sendToPlayer(new ClientboundCastError(ClientboundCastError.CastErrorMessages.MANA.id, this.spellType.getValue()), serverPlayer);
                return false;
            }

            if (this.castType == CastType.INSTANT) {
                /*
                 * Immediately cast spell
                 */
                castSpell(level, serverPlayer, castSource, triggerCooldown);
            } else if (this.castType == CastType.LONG || this.castType == CastType.CONTINUOUS || this.castType == CastType.CHARGE) {
                //TODO: effective cast time needs better logic (it reduces continuous cast duration and will need to be utilized in faster charge casting)
                /*
                 * Prepare to cast spell (magic manager will pick it up by itself)
                 */
                int effectiveCastTime = getEffectiveCastTime(player);
                playerMagicData.initiateCast(getID(), this.level, effectiveCastTime, castSource);
                onServerPreCast(player.level, player, playerMagicData);
                Messages.sendToPlayer(new ClientboundUpdateCastingState(getID(), getLevel(), effectiveCastTime, castSource, false), serverPlayer);
            }

            Messages.sendToPlayersTrackingEntity(new ClientboundOnCastStarted(serverPlayer.getUUID(), spellType), serverPlayer, true);

            return true;
        } else {
            Utils.serverSideCancelCast(serverPlayer);
            return false;
        }
    }

    public void castSpell(Level world, ServerPlayer serverPlayer, CastSource castSource, boolean triggerCooldown) {
        MagicManager magicManager = MagicManager.get(serverPlayer.level);
        PlayerMagicData playerMagicData = PlayerMagicData.getPlayerMagicData(serverPlayer);

        if (castSource.consumesMana()) {
            //TODO: sword mana multiplier?
            int newMana = playerMagicData.getMana() - getManaCost();
            magicManager.setPlayerCurrentMana(serverPlayer, newMana);
            Messages.sendToPlayer(new ClientboundSyncMana(playerMagicData), serverPlayer);
        }

        if (triggerCooldown) {
            MagicManager.get(serverPlayer.level).addCooldown(serverPlayer, spellType, castSource);
        }

        Messages.sendToPlayer(new ClientboundOnClientCast(this.getID(), this.level, castSource), serverPlayer);

        onCast(world, serverPlayer, playerMagicData);

        if (this.castType != CastType.CONTINUOUS) {
            onServerCastComplete(world, serverPlayer, playerMagicData);
        }

        if (serverPlayer.getMainHandItem().getItem() instanceof SpellBook || serverPlayer.getMainHandItem().getItem() instanceof Scroll)
            playerMagicData.setPlayerCastingItem(serverPlayer.getMainHandItem());
        else
            playerMagicData.setPlayerCastingItem(serverPlayer.getOffhandItem());

    }

//    private int getCooldownLength(ServerPlayer serverPlayer) {
//        double playerCooldownModifier = serverPlayer.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION.get());
//        return MagicManager.getEffectiveSpellCooldown(cooldown, playerCooldownModifier);
//    }

    /**
     * The primary spell effect sound and particle handling goes here. Called Client Side only
     */
    public void onClientCastComplete(Level level, LivingEntity entity, PlayerMagicData playerMagicData) {
        //irons_spellbooks.LOGGER.debug("AbstractSpell.: onClientCast:{}", level.isClientSide);
        playSound(getCastFinishSound(), entity, true);
        if (ClientInputEvents.isUseKeyDown) {
            if (this.spellType.getCastType().holdToCast()) {
                ClientSpellCastHelper.setSuppressRightClicks(true);
            }
            ClientInputEvents.hasReleasedSinceCasting = false;
        }
    }

    /**
     * The primary spell effect handling goes here. Called Server Side
     */
    public void onCast(Level level, LivingEntity entity, PlayerMagicData playerMagicData) {
        playSound(getCastFinishSound(), entity, true);
    }

    private void playSound(Optional<SoundEvent> sound, Entity entity, boolean playDefaultSound) {
        IronsSpellbooks.LOGGER.debug("playSound spell:{} isClientSide:{}", this.getSpellType(), entity.level.isClientSide);
        // sound.ifPresent((soundEvent) -> entity.playSound(soundEvent, 1.0f, 1.0f));
        if (sound.isPresent())
            entity.playSound(sound.get(), 1.0f, 1.0f);
        else if (playDefaultSound)
            entity.playSound(defaultCastSound(), 1.0f, 1.0f);

        //entity.playSound(sound.orElse(this:def), 1.0f, 1.0f));
    }

    private void playSound(Optional<SoundEvent> sound, Entity entity) {
        playSound(sound, entity, false);
    }

    private SoundEvent defaultCastSound() {
        return switch (this.getSchoolType()) {

            case FIRE -> SoundRegistry.FIRE_CAST.get();
            case ICE -> SoundRegistry.ICE_CAST.get();
            case LIGHTNING -> SoundRegistry.LIGHTNING_CAST.get();
            case HOLY -> SoundRegistry.HOLY_CAST.get();
            case ENDER -> SoundRegistry.ENDER_CAST.get();
            case BLOOD -> SoundRegistry.BLOOD_CAST.get();
            case EVOCATION -> SoundRegistry.EVOCATION_CAST.get();
            default -> SoundRegistry.EVOCATION_CAST.get();
        };
    }

    /**
     * Called on the server when a spell finishes casting or is cancelled, used for any cleanup or extra functionality
     */
    public void onServerCastComplete(Level level, LivingEntity entity, PlayerMagicData playerMagicData) {
    }

    /**
     * Called once just before executing onCast. Can be used for client side sounds and particles
     */
    public void onClientPreCast(Level level, LivingEntity entity, InteractionHand hand, @Nullable PlayerMagicData playerMagicData) {
        //irons_spellbooks.LOGGER.debug("AbstractSpell.onClientPreCast: isClient:{} entity:{}", level.isClientSide, entity);
        playSound(getCastStartSound(), entity);
    }

    /**
     * Called once just before executing onCast. Can be used for server side sounds and particles
     */
    public void onServerPreCast(Level level, LivingEntity entity, @Nullable PlayerMagicData playerMagicData) {
        //irons_spellbooks.LOGGER.debug("AbstractSpell.: onServerPreCast:{}", level.isClientSide);
        playSound(getCastStartSound(), entity);
    }

    /**
     * Called on the server each tick while casting
     */
    public void onServerCastTick(Level level, LivingEntity entity, @Nullable PlayerMagicData playerMagicData) {

    }

    public List<MutableComponent> getUniqueInfo() {
        return uniqueInfo;
    }

    @Override
    public boolean equals(Object obj) {
        AbstractSpell o = (AbstractSpell) obj;
        if (o == null)
            return false;
        return this.spellType == o.spellType && this.level == o.level;
    }
}