package me.libraryaddict.disguise.utilities.reflection.v1_18_1;

import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers.Direction;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import me.libraryaddict.disguise.utilities.reflection.ReflectionManagerAbstract;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vector3f;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Art;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R1.CraftArt;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;
import org.bukkit.craftbukkit.v1_18_R1.CraftSound;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftNamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ReflectionManager implements ReflectionManagerAbstract {
    public boolean hasInvul(Entity entity) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();

        if (nmsEntity instanceof net.minecraft.world.entity.LivingEntity) {
            return nmsEntity.invulnerableTime > 0;
        } else {
            return nmsEntity.isInvulnerableTo(DamageSource.GENERIC);
        }
    }

    public int getIncrementedStateId(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        return serverPlayer.containerMenu.incrementStateId(); // TODO Check correct container
    }

    public int getNewEntityId() {
        return getNewEntityId(true);
    }

    public int getNewEntityId(boolean increment) {
        try {
            Field entityCounter = net.minecraft.world.entity.Entity.class.getDeclaredField("b");
            entityCounter.setAccessible(true);
            AtomicInteger atomicInteger = (AtomicInteger) entityCounter.get(null);
            if (increment) {
                return atomicInteger.incrementAndGet();
            } else {
                return atomicInteger.get();
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public ServerGamePacketListenerImpl getPlayerConnectionOrPlayer(Player player) {
        return ((CraftPlayer) player).getHandle().connection;
    }

    public net.minecraft.world.entity.Entity createEntityInstance(String entityName) {
        Optional<net.minecraft.world.entity.EntityType<?>> optional = net.minecraft.world.entity.EntityType.byString(entityName.toLowerCase(Locale.ROOT));
        if (optional.isPresent()) {
            net.minecraft.world.entity.EntityType<?> entityType = optional.get();
            ServerLevel world = getWorldServer(Bukkit.getWorlds().get(0));
            net.minecraft.world.entity.Entity entity;
            if (entityType == net.minecraft.world.entity.EntityType.PLAYER) {
                WrappedGameProfile gameProfile = ReflectionManagerAbstract.getGameProfile(new UUID(0, 0), "Steve");
                entity = new ServerPlayer(getMinecraftServer(), world, (GameProfile) gameProfile.getHandle());
            }/* else if (entityType == net.minecraft.world.entity.EntityType.ENDER_PEARL) {
                entity = new ThrownEnderpearl(world, (net.minecraft.world.entity.LivingEntity) createEntityInstance("cow"));
            } else if (entityType == net.minecraft.world.entity.EntityType.FISHING_BOBBER) {
                entity = new FishingHook((net.minecraft.world.entity.player.Player) createEntityInstance("player"), world, 0, 0);
            }*/ else {
                entity = entityType.create(world);
            }

            // Workaround for paper being 2 smart 4 me
            entity.setPos(1.0, 1.0, 1.0);
            entity.setPos(0.0, 0.0, 0.0);
            return entity;
        }

        return null;
    }

    public MobEffect getMobEffectList(int id) {
        return MobEffect.byId(id);
    }

    public MobEffectInstance createMobEffect(PotionEffect effect) {
        return createMobEffect(effect.getType().getId(), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles());
    }

    public MobEffectInstance createMobEffect(int id, int duration, int amplification, boolean ambient, boolean particles) {
        return new MobEffectInstance(getMobEffectList(id), duration, amplification, ambient, particles);
    }

    public AABB getBoundingBox(Entity entity) {
        return ((CraftEntity) entity).getHandle().getBoundingBox();
    }

    public double getXBoundingBox(Entity entity) {
        return getBoundingBox(entity).maxX - getBoundingBox(entity).minX;
    }

    public double getYBoundingBox(Entity entity) {
        return getBoundingBox(entity).maxY - getBoundingBox(entity).minY;
    }

    public double getZBoundingBox(Entity entity) {
        return getBoundingBox(entity).maxZ - getBoundingBox(entity).minZ;
    }

    public ServerPlayer getPlayerFromPlayerConnection(Object nmsEntity) {
        return ((ServerPlayerConnection) nmsEntity).getPlayer();
    }

    public Entity getBukkitEntity(Object nmsEntity) {
        return ((net.minecraft.world.entity.Entity) nmsEntity).getBukkitEntity();
    }

    public ItemStack getBukkitItem(Object nmsItem) {
        return CraftItemStack.asBukkitCopy((net.minecraft.world.item.ItemStack) nmsItem);
    }

    public ItemStack getCraftItem(ItemStack bukkitItem) {
        return CraftItemStack.asCraftCopy(bukkitItem);
    }

    public SoundEvent getCraftSound(Sound sound) {
        return CraftSound.getSoundEffect(sound);
    }

    public ServerEntity getEntityTrackerEntry(Entity target) throws Exception {
        ServerLevel world = ((CraftWorld) target.getWorld()).getHandle();
        ServerChunkCache chunkSource = world.getChunkSource();
        ChunkMap chunkMap = chunkSource.chunkMap;
        Map<Integer, ChunkMap.TrackedEntity> entityMap = chunkMap.entityMap;
        ChunkMap.TrackedEntity trackedEntity = entityMap.get(target.getEntityId());
        if (trackedEntity == null) {
            return null;
        }

        Field field = ChunkMap.TrackedEntity.class.getDeclaredField("b");
        field.setAccessible(true);

        return (ServerEntity) field.get(trackedEntity);
    }

    public DedicatedServer getMinecraftServer() {
        return ((CraftServer) Bukkit.getServer()).getServer();
    }

    public String getEnumArt(Art art) {
        return Registry.MOTIVE.getKey(CraftArt.BukkitToNotch(art)).getPath();
    }

    public BlockPos getBlockPosition(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    public net.minecraft.core.Direction getEnumDirection(int direction) {
        return net.minecraft.core.Direction.from2DDataValue(direction);
    }

    public ClientboundPlayerInfoPacket.Action getEnumPlayerInfoAction(int action) {
        return ClientboundPlayerInfoPacket.Action.values()[action];
    }

    public ClientboundPlayerInfoPacket.PlayerUpdate getPlayerInfoData(WrappedGameProfile gameProfile) {
        return new ClientboundPlayerInfoPacket.PlayerUpdate((GameProfile) gameProfile.getHandle(), 0, GameType.SURVIVAL,
            new TextComponent(gameProfile.getName()));
    }

    public Object getNmsEntity(Entity entity) {
        return ((CraftEntity) entity).getHandle();
    }

    public double getPing(Player player) {
        return player.getPing();
    }

    public float[] getSize(Entity entity) {
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        EntityDimensions dimensions = nmsEntity.getDimensions(net.minecraft.world.entity.Pose.STANDING);
        return new float[]{dimensions.width, nmsEntity.getEyeHeight()};
    }

    public WrappedGameProfile getSkullBlob(WrappedGameProfile gameProfile) {
        DedicatedServer minecraftServer = getMinecraftServer();
        MinecraftSessionService sessionService = minecraftServer.getSessionService();
        return WrappedGameProfile.fromHandle(sessionService.fillProfileProperties((GameProfile) gameProfile.getHandle(), true));
    }

    public Float getSoundModifier(Object entity) {
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity)) {
            return 0.0f;
        } else {
            try {
                Method method = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("eu");
                method.setAccessible(true);

                return (Float) method.invoke(entity);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return 0f;
    }

    public void injectCallback(String playername, ProfileLookupCallback callback) {
        Agent agent = Agent.MINECRAFT;
        getMinecraftServer().getProfileRepository().findProfilesByNames(new String[]{playername}, agent, callback);
    }

    public void setBoundingBox(Entity entity, double x, double y, double z) {
        Location loc = entity.getLocation();
        ((CraftEntity) entity).getHandle()
            .setBoundingBox(new AABB(loc.getX() - x / 2, loc.getY() - y / 2, loc.getZ() - z / 2, loc.getX() + x / 2, loc.getY() + y / 2, loc.getZ() + z / 2));
    }

    public Enum getSoundCategory(String category) {
        return Arrays.stream(SoundSource.values()).filter(soundSource -> category.equalsIgnoreCase(soundSource.getName())).findAny().get();
    }

    /**
     * Creates the NMS object EnumItemSlot from an EquipmentSlot.
     *
     * @param slot
     * @return null if the equipment slot is null
     */
    public Enum createEnumItemSlot(EquipmentSlot slot) {
        switch (slot) {
            case HAND:
                return net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFF_HAND:
                return net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case FEET:
                return net.minecraft.world.entity.EquipmentSlot.FEET;
            case LEGS:
                return net.minecraft.world.entity.EquipmentSlot.LEGS;
            case CHEST:
                return net.minecraft.world.entity.EquipmentSlot.CHEST;
            case HEAD:
                return net.minecraft.world.entity.EquipmentSlot.HEAD;
            default:
                return null;
        }
    }

    public Object getSoundString(Sound sound) {
        return CraftSound.getSoundEffect(sound).getLocation().toString(); // TODO
    }

    public Optional<?> convertOptional(Object val) {
        if (val instanceof BlockPosition) {
            BlockPosition pos = (BlockPosition) val;
            return Optional.of(getBlockPosition(pos.getX(), pos.getY(), pos.getZ()));
        } else if (val instanceof WrappedBlockData) {
            Object obj = ((WrappedBlockData) val).getHandle();
            return Optional.of(obj);
        } else if (val instanceof ItemStack) {
            Object obj = getNmsItem((ItemStack) val);
            return Optional.of(obj);
        } else if (val instanceof WrappedChatComponent) {
            Object obj = ((WrappedChatComponent) val).getHandle();
            return Optional.of(obj);
        }

        return Optional.of(val);
    }

    public Vector3f convertVec3(Object object) {
        if (object instanceof Vector3F) {
            Vector3F vector3F = (Vector3F) object;
            return new Vector3f(vector3F.getX(), vector3F.getY(), vector3F.getZ());
        } else if (object instanceof EulerAngle) {
            EulerAngle eulerAngle = (EulerAngle) object;
            return new Vector3f((float) eulerAngle.getX(), (float) eulerAngle.getY(), (float) eulerAngle.getZ());
        }

        return null;
    }

    public net.minecraft.core.Direction convertDirection(Direction direction) {
        return net.minecraft.core.Direction.from3DDataValue(direction.ordinal());
    }

    public Material getMaterial(String name) {
        return CraftMagicNumbers.INSTANCE.getMaterial(name, CraftMagicNumbers.INSTANCE.getDataVersion());
    }

    public String getItemName(Material material) {
        return Registry.ITEM.getKey(CraftMagicNumbers.getItem(material)).getPath();
    }

    public net.minecraft.world.item.ItemStack getNmsItem(ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    public VillagerData getNmsVillagerData(Villager.Type villagerType, Villager.Profession villagerProfession) {
        VillagerType nmsVillagerType = Registry.VILLAGER_TYPE.get(CraftNamespacedKey.toMinecraft(villagerType.getKey()));
        VillagerProfession nmsVillagerProfession = Registry.VILLAGER_PROFESSION.get(CraftNamespacedKey.toMinecraft(villagerProfession.getKey()));

        return new net.minecraft.world.entity.npc.VillagerData(nmsVillagerType, nmsVillagerProfession, 1);
    }

    public VillagerType getVillagerType(Villager.Type type) {
        return Registry.VILLAGER_TYPE.get(CraftNamespacedKey.toMinecraft(type.getKey()));
    }

    public VillagerProfession getVillagerProfession(Villager.Profession profession) {
        return Registry.VILLAGER_PROFESSION.get(CraftNamespacedKey.toMinecraft(profession.getKey()));
    }

    public <T> SynchedEntityData.DataItem<T> createDataWatcherItem(WrappedDataWatcher.WrappedDataWatcherObject wrappedDataWatcherObject, T metaItem) {
        return new SynchedEntityData.DataItem<>((EntityDataAccessor<T>) wrappedDataWatcherObject.getHandle(), metaItem);
    }

    @Deprecated
    public SoundEvent createSoundEffect(String minecraftKey) {
        return new SoundEvent(new ResourceLocation(minecraftKey));
    }

    @Override
    public ResourceLocation createMinecraftKey(String name) {
        return new ResourceLocation(name);
    }

    public Vec3 getVec3D(Vector vector) {
        return new Vec3(vector.getX(), vector.getY(), vector.getZ());
    }

    public net.minecraft.world.entity.EntityType getEntityType(EntityType entityType) {
        return net.minecraft.world.entity.EntityType.byString(
            entityType.getName() == null ? entityType.name().toLowerCase(Locale.ENGLISH) : entityType.getName()).orElse(null);
    }

    public Object registerEntityType(NamespacedKey key) {
        net.minecraft.world.entity.EntityType<net.minecraft.world.entity.Entity> newEntity =
            new net.minecraft.world.entity.EntityType<>(null, null, false, false, false, false, null, null, 0, 0);
        Registry.register(Registry.ENTITY_TYPE, CraftNamespacedKey.toMinecraft(key), newEntity);
        newEntity.getDescriptionId();
        return newEntity; // TODO ??? Some reflection in legacy that I'm unsure about
    }

    public int getEntityTypeId(Object entityTypes) {
        net.minecraft.world.entity.EntityType entityType = (net.minecraft.world.entity.EntityType) entityTypes;
        return Registry.ENTITY_TYPE.getId(entityType);
    }

    public int getEntityTypeId(EntityType entityType) {
        return getEntityTypeId(getEntityType(entityType));
    }

    public Object getEntityType(NamespacedKey name) {
        return Registry.ENTITY_TYPE.get(CraftNamespacedKey.toMinecraft(name));
    }

    public Object getNmsEntityPose(String enumPose) {
        return net.minecraft.world.entity.Pose.valueOf(enumPose);
    }

    public int getCombinedIdByBlockData(BlockData data) {
        BlockState state = ((CraftBlockData) data).getState();
        return Block.getId(state);
    }

    public int getCombinedIdByItemStack(ItemStack itemStack) {
        Block block = CraftMagicNumbers.getBlock(itemStack.getType());
        return Block.getId(block.defaultBlockState());
    }

    public BlockData getBlockDataByCombinedId(int id) {
        return CraftBlockData.fromData(Block.stateById(id));
    }

    public ItemStack getItemStackByCombinedId(int id) {
        return new ItemStack(CraftMagicNumbers.getMaterial(Block.stateById(id).getBlock()));
    }

    public ServerLevel getWorldServer(World w) {
        return ((CraftWorld) w).getHandle();
    }

    public ItemMeta getDeserializedItemMeta(Map<String, Object> meta) {
        try {
            Class<?> aClass = Class.forName("org.bukkit.craftbukkit.v1_18_R1.inventory.CraftMetaItem$SerializableMeta");
            Method deserialize = aClass.getDeclaredMethod("deserialize", Map.class);
            Object itemMeta = deserialize.invoke(null, meta);

            return (ItemMeta) itemMeta;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
