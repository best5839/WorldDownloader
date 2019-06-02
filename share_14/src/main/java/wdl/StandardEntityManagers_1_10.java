/*
 * This file is part of World Downloader: A mod to make backups of your
 * multiplayer worlds.
 * http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2520465
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2017-2018 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see http://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package wdl;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nonnull;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.item.ExperienceBottleEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.EyeOfEnderEntity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.item.FireworkRocketEntity;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.DamagingProjectileEntity;
import net.minecraft.entity.projectile.EggEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wdl.EntityUtils.ISpigotEntityManager;
import wdl.EntityUtils.SpigotEntityType;


import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityEnderEye;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.entity.projectile.EntitySnowball;
import wdl.EntityUtils.ISpigotEntityManager;
import wdl.EntityUtils.SpigotEntityType;
import wdl.api.IEntityManager;

/**
 * Standard implementations of IEntityManager. These implementations are used on
 * 1.10 and below, before entities were refactored to use namespaced identifiers
 * (and before forge made any changes to EntityList).
 */
class StandardEntityManagers {
	private StandardEntityManagers() { throw new AssertionError(); }

	// For most entities, we want them to be enabled by default. A few dangerous
	// entities should't be saved, though.
	private static final Set<String> DANGEROUS_ENTITIES = ImmutableSet.of(
			"FireworksRocketEntity",
			"EnderDragon",
			"WitherBoss",
			"PrimedTnt");

	public static final ISpigotEntityManager SPIGOT = new ISpigotEntityManager() {
		@Override
		public Set<String> getProvidedEntities() {
			if (WDL.INSTANCE.isSpigot()) {
				return PROVIDED_ENTITIES;
			} else {
				// Don't try to do spigot ranges on non-spigot servers
				return Collections.emptySet();
			}
		}

		@Override
		public String getIdentifierFor(Entity entity) {
			// Handled by default
			return null;
		}

		@Override
		public int getTrackDistance(String identifier, Entity entity) {
			return getSpigotType(identifier).getDefaultRange();
		}

		@Nonnull
		@Override
		public SpigotEntityType getSpigotType(String identifier) {
			Class<? extends Entity> c = entityClassFor(this, identifier);
			if (c == null) {
				return SpigotEntityType.UNKNOWN;
			}

			// Spigot's mapping, which is based off of bukkit inheritance (which
			// doesn't match vanilla)
			if (MonsterEntity.class.isAssignableFrom(c) ||
					SlimeEntity.class.isAssignableFrom(c)) {
				return SpigotEntityType.MONSTER;
			} else if (CreatureEntity.class.isAssignableFrom(c) ||
					AmbientEntity.class.isAssignableFrom(c)) {
				return SpigotEntityType.ANIMAL;
			} else if (ItemFrameEntity.class.isAssignableFrom(c) ||
					PaintingEntity.class.isAssignableFrom(c) ||
					ItemEntity.class.isAssignableFrom(c) ||
					ExperienceOrbEntity.class.isAssignableFrom(c)) {
				return SpigotEntityType.MISC;
			} else {
				return SpigotEntityType.OTHER;
			}
		}

		@Override
		public boolean enabledByDefault(String identifier) {
			return !DANGEROUS_ENTITIES.contains(identifier);
		}

		// Not intended to be used as a regular extension, so don't worry about
		// these methods
		@Override
		public boolean isValidEnvironment(String version) {
			return true;
		}
		@Override
		public String getEnvironmentErrorMessage(String version) {
			return null;
		}

		@Override
		public String getGroup(String identifier) {
			return null;
		}

		@Override
		public String getDisplayIdentifier(String identifier) {
			return null;
		}

		@Override
		public String getDisplayGroup(String group) {
			return null;
		}
	};

	public static final IEntityManager VANILLA = new IEntityManager() {
		@Override
		public Set<String> getProvidedEntities() {
			return PROVIDED_ENTITIES;
		}

		@Override
		public String getIdentifierFor(Entity entity) {
			String id = EntityList.getEntityString(entity);
			if (id == null) {
				// Happens with players.  This check isn't needed in 1.10 though
				return null;
			} else {
				return id;
			}
		}

		/**
		 * Gets the entity tracking range used by vanilla Minecraft.
		 * <p>
		 * Proper tracking ranges can be found in
		 * {@link EntityTracker#track(Entity)} - it's the 2nd argument given to
		 * addEntityToTracker.  These ranges need to be checked for each new entity.
		 * <p>
		 * This code can be generated via replacing
		 * <pre>\t+\} else if \(entityIn instanceof (.+)\) \{\r?\n\t+this\.track\(entityIn, (\d+), .+, (?:false|true)\);</pre>
		 * with
		 * <pre>\t\t\t} else if \(\1.class.isAssignableFrom\(c\)\) {\r\n\t\t\t\treturn \2;</pre>
		 * (and then doing some manual clean up)
		 */
		@Override
		public int getTrackDistance(String identifier, Entity entity) {
			Class<? extends Entity> c = entityClassFor(this, identifier);
			if (c == null) {
				return -1;
			}

			if (FishingBobberEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (AbstractArrowEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (SmallFireballEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (DamagingProjectileEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (SnowballEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (EnderPearlEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (EyeOfEnderEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (EggEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (PotionEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (ExperienceBottleEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (FireworkRocketEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (ItemEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (AbstractMinecartEntity.class.isAssignableFrom(c)) {
				return 80;
			} else if (BoatEntity.class.isAssignableFrom(c)) {
				return 80;
			} else if (SquidEntity.class.isAssignableFrom(c)) {
				return 64;
			} else if (WitherEntity.class.isAssignableFrom(c)) {
				return 80;
			} else if (ShulkerBulletEntity.class.isAssignableFrom(c)) {
				return 80;
			} else if (BatEntity.class.isAssignableFrom(c)) {
				return 80;
			} else if (EnderDragonEntity.class.isAssignableFrom(c)) {
				return 160;
			} else if (IAnimals.class.isAssignableFrom(c)) {
				return 80;
			} else if (TNTEntity.class.isAssignableFrom(c)) {
				return 160;
			} else if (FallingBlockEntity.class.isAssignableFrom(c)) {
				return 160;
			} else if (HangingEntity.class.isAssignableFrom(c)) {
				return 160;
			} else if (ArmorStandEntity.class.isAssignableFrom(c)) {
				return 160;
			} else if (ExperienceOrbEntity.class.isAssignableFrom(c)) {
				return 160;
			} else if (AreaEffectCloudEntity.class.isAssignableFrom(c)) {
				return 160;
			} else if (EnderCrystalEntity.class.isAssignableFrom(c)) {
				return 256;
			} else {
				return -1;
			}
		}

		@Override
		public String getGroup(String identifier) {
			Class<? extends Entity> c = entityClassFor(this, identifier);
			if (c == null) {
				return null;
			}

			if (IMob.class.isAssignableFrom(c)) {
				return "Hostile";
			} else if (IAnimals.class.isAssignableFrom(c)) {
				return "Passive";
			} else {
				return "Other";
			}
		}

		@Override
		public String getDisplayIdentifier(String identifier) {
			String i18nKey = "entity." + identifier + ".name";
			if (I18n.hasKey(i18nKey)) {
				return I18n.format(i18nKey);
			} else {
				// We want to be clear that there is no result, rather than returning
				// the key (the default for failed formatting)
				// Note that some entities do not have translation strings (https://bugs.mojang.com/browse/MC-68446)
				return null;
			}
		}

		@Override
		public String getDisplayGroup(String group) {
			// TODO
			return null;
		}

		@Override
		public boolean enabledByDefault(String identifier) {
			return !DANGEROUS_ENTITIES.contains(identifier);
		}

		// Not intended to be used as a regular extension, so don't worry about
		// these methods
		@Override
		public boolean isValidEnvironment(String version) {
			return true;
		}
		@Override
		public String getEnvironmentErrorMessage(String version) {
			return null;
		}
	};

	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * @see EntityList#field_75625_b
	 */
	private static final Map<String, Class<? extends Entity>> typeToClassMap;
	static {
		// Unfortunately there is no getter for what we need

		try {
			Map<String, Class<? extends Entity>> result = null;

			for (Field field : EntityList.class.getDeclaredFields()) {
				if (field.getType().equals(Map.class)) {
					field.setAccessible(true);
					Map<?, ?> map = (Map<?, ?>) field.get(null);
					// We know it's the right map if the keys are strings and
					// the values are classes
					Map.Entry<?, ?> item = map.entrySet().iterator().next();
					if (item.getKey() instanceof String
							&& item.getValue() instanceof Class<?>) {
						@SuppressWarnings("unchecked")
						Map<String, Class<? extends Entity>> temp =
						(Map<String, Class<? extends Entity>>) map;
						result = temp;
						break;
					}
				}
			}
			if (result == null) {
				throw new RuntimeException("Couldn't find type to class map");
			} else {
				typeToClassMap = result;
			}
		} catch (Throwable ex) {
			LOGGER.error("[WDL] Failed to set up entity mappings: ", ex);
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Gets the entity class for that identifier.
	 */
	@VisibleForTesting
	static Class<? extends Entity> entityClassFor(IEntityManager manager, String identifier) {
		assert manager.getProvidedEntities().contains(identifier);

		if (!EntityList.getEntityNameList().contains(identifier)) {
			return null;
		}

		Class<? extends Entity> c = typeToClassMap.get(identifier);
		assert c != null;

		return c;
	}

	/**
	 * As returned by {@link #getProvidedEntities()}
	 */
	private static final Set<String> PROVIDED_ENTITIES;
	static {
		try {
			ImmutableSet.Builder<String> builder = ImmutableSet.builder();
			for (String loc : EntityList.getEntityNameList()) {
				if (loc.equals("LightningBolt")) continue;

				builder.add(loc);
			}
			PROVIDED_ENTITIES = builder.build();
		} catch (Throwable ex) {
			LOGGER.error("[WDL] Failed to load entity list: ", ex);
			throw new RuntimeException(ex);
		}
	}
}
