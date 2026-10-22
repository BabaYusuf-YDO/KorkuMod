package com.korkumod.main;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Random;

@Mod("korkumodu")
@Mod.EventBusSubscriber(modid = "korkumodu")
public class KorkuModuTamPaket {

    private static final Random RANDOM = new Random();

    // --- REGISTRY (VARLIK VE EŞYA KAYITLARI) ---
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "korkumodu");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "korkumodu");

    // NULL VARLIĞI
    public static final RegistryObject<EntityType<NullEntity>> NULL_ENTITY = ENTITIES.register("null_entity",
            () -> EntityType.Builder.of(NullEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .build("null_entity"));

    // PERVERT VARLIĞI
    public static final RegistryObject<EntityType<PervertEntity>> PERVERT_ENTITY = ENTITIES.register("pervert_entity",
            () -> EntityType.Builder.of(PervertEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .build("pervert_entity"));

    // EL FENERİ EŞYASI
    public static final RegistryObject<Item> FLASHLIGHT = ITEMS.register("flashlight",
            () -> new FlashlightItem(new Item.Properties().stacksTo(1)));

    // SAYAÇ DEĞİŞKENLERİ
    private static int sleeplessTicks = 0;
    private static boolean insomniaActive = false;
    private static int insomniaCountdown = 59;
    private static int insomniaTicker = 0;

    private static int pervertSpawnTimer = 0;
    private static boolean pervertActive = false;
    private static int pervertReactionTimer = 0;
    private static PervertEntity activePervert = null;

    // ==========================================
    // 1. NULL YARATIK SINIFI (Saldırgan Varlık)
    // ==========================================
    public static class NullEntity extends PathfinderMob {
        public NullEntity(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes()
                    .add(Attributes.MAX_HEALTH, 100.0D)
                    .add(Attributes.MOVEMENT_SPEED, 0.4D)
                    .add(Attributes.ATTACK_DAMAGE, 20.0D);
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(1, new FloatGoal(this));
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
            this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    // ==========================================
    // 2. PERVERT YARATIK SINIFI (Stalker / İzleyici)
    // ==========================================
    public static class PervertEntity extends PathfinderMob {
        public PervertEntity(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes()
                    .add(Attributes.MAX_HEALTH, 500.0D)
                    .add(Attributes.MOVEMENT_SPEED, 0.0D); // Sabit durur, izler
        }
    }

    // ==========================================
    // 3. EL FENERİ SINIFI (Özellik 1)
    // ==========================================
    public static class FlashlightItem extends Item {
        public FlashlightItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            CompoundTag tag = stack.getOrCreateTag();
            boolean isOn = tag.getBoolean("is_on");
            tag.putBoolean("is_on", !isOn);

            if (!level.isClientSide) {
                String status = !isOn ? "§aEl Feneri Açıldı" : "§cEl Feneri Kapandı";
                player.sendSystemMessage(Component.literal(status));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
    }

    // ==========================================
    // 4. RENDERER / SKIN KAPLAMA BAĞLANTILARI
    // ==========================================
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Null Skin Bağlantısı
        event.registerEntityRenderer(NULL_ENTITY.get(), context ->
                new HumanoidMobRenderer<>(context, new net.minecraft.client.model.PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f) {
                    @Override
                    public ResourceLocation getTextureLocation(NullEntity entity) {
                        return new ResourceLocation("korkumodu", "textures/entity/null_skin.png");
                    }
                });

        // Pervert Skin Bağlantısı
        event.registerEntityRenderer(PERVERT_ENTITY.get(), context ->
                new HumanoidMobRenderer<>(context, new net.minecraft.client.model.PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f) {
                    @Override
                    public ResourceLocation getTextureLocation(PervertEntity entity) {
                        return new ResourceLocation("korkumodu", "textures/entity/pervert_skin.png");
                    }
                });
    }

    // ==========================================
    // 5. OYUN İÇİ OLAYLAR VE MEKANİKLER
    // ==========================================

    // DOĞUŞTA EL FENERİ VERME
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        CompoundTag playerData = player.getPersistentData();

        if (!playerData.getBoolean("ReceivedFlashlight")) {
            player.getInventory().add(new ItemStack(FLASHLIGHT.get()));
            playerData.putBoolean("ReceivedFlashlight", true);
        }
    }

    // ÖZELLİK 2: YATAK KARABASANI (%10 Şans)
    @SubscribeEvent
    public static void onPlayerSleep(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (!level.isClientSide && level.isNight() && isBed(event)) {
            if (RANDOM.nextInt(10) == 0) { // %10 İhtimal
                event.setCanceled(true);

                // Boşluğa çek ve dondur
                player.teleportTo(player.getX(), player.getY() - 50, player.getZ());
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 255, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 1, false, false));

                // Null'u oyuncunun tam karşısında spawn et
                NullEntity nullMob = NULL_ENTITY.get().create(level);
                if (nullMob != null) {
                    nullMob.moveTo(player.getX(), player.getY() - 50, player.getZ() + 2, 0, 0);
                    level.addFreshEntity(nullMob);
                }

                // 2.5 saniye sonra "See You" yazıp öldür
                player.getServer().execute(() -> {
                    try { Thread.sleep(2500); } catch (Exception ignored) {}
                    player.sendSystemMessage(Component.literal("§cSee You"));
                    player.hurt(level.damageSources().outOfWorld(), 1000f);
                });
            }
        }
    }

    private static boolean isBed(PlayerInteractEvent.RightClickBlock event) {
        return event.getLevel().getBlockState(event.getPos()).getBlock().getDescriptionId().contains("bed");
    }

    // ÖZELLİK 4: TOTEM YANILSAMASI (%10 Şans)
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            Level level = player.level();
            boolean hasTotem = player.getMainHandItem().is(Items.TOTEM_OF_UNDYING) || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);

            if (hasTotem && RANDOM.nextInt(10) == 0) { // %10 Şans
                event.setCanceled(true);

                // Totemi sil ve oyuncuyu dondur
                if (player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) player.getMainHandItem().shrink(1);
                else player.getOffhandItem().shrink(1);

                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 255, false, false));

                // Null tam önünde belirir
                NullEntity nullMob = NULL_ENTITY.get().create(level);
                if (nullMob != null) {
                    nullMob.moveTo(player.getX(), player.getY(), player.getZ() + 1, 0, 0);
                    level.addFreshEntity(nullMob);
                }

                player.sendSystemMessage(Component.literal("§4Null totemi elinden alıp yok etti..."));

                // Jumpscare olmadan boğarak öldür
                player.getServer().execute(() -> {
                    try { Thread.sleep(2500); } catch (Exception ignored) {}
                    player.hurt(level.damageSources().magic(), 1000f);
                });
            }
        }
    }

    // ÖZELLİK 3 VE 5: TİCK SİSTEMİ (Uykusuzluk & Pervert Stalker)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        Level level = player.level();
        if (level.isClientSide) return;

        // --- ÖZELLİK 3: UYKUSUZLUK SAYAÇ MANTIĞI ---
        if (player.isSleeping()) {
            sleeplessTicks = 0;
            if (insomniaActive) {
                player.sendSystemMessage(Component.literal("§aYou Made It"));
                insomniaActive = false;
            }
        } else {
            sleeplessTicks++;
            if (sleeplessTicks >= 48000 && !insomniaActive) { // 2 Gece Uykusuzluk
                insomniaActive = true;
                insomniaCountdown = 59;
                player.sendSystemMessage(Component.literal("§4§l1 Minute To Sleep"));
            }

            if (insomniaActive) {
                insomniaTicker++;
                if (insomniaTicker >= 20) { // Her 1 saniyede
                    insomniaTicker = 0;
                    insomniaCountdown--;

                    if (insomniaCountdown > 0) {
                        player.sendSystemMessage(Component.literal("§eUyuman İçin Kalan Süre: " + insomniaCountdown));
                    } else {
                        // Süre Bitti! Null Spawn Olur ve Dondurur
                        insomniaActive = false;
                        player.sendSystemMessage(Component.literal("§4§lTime's Up!"));

                        NullEntity nullMob = NULL_ENTITY.get().create(level);
                        if (nullMob != null) {
                            nullMob.moveTo(player.getX(), player.getY(), player.getZ() - 1, 0, 0);
                            level.addFreshEntity(nullMob);
                        }

                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 255, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 1, false, false));

                        player.getServer().execute(() -> {
                            try { Thread.sleep(3000); } catch (Exception ignored) {}
                            player.hurt(level.damageSources().generic(), 1000f);
                        });
                    }
                }
            }
        }

        // --- ÖZELLİK 5: PERVERT STALKER MANTIĞI ---
        pervertSpawnTimer++;
        if (pervertSpawnTimer >= 12000 && !pervertActive) { // 10 Dakikada bir
            pervertSpawnTimer = 0;

            // Uzakta Pervert Spawn Et (20 Blok Ötede)
            activePervert = PERVERT_ENTITY.get().create(level);
            if (activePervert != null) {
                activePervert.moveTo(player.getX() + 15, player.getY(), player.getZ() + 15, 0, 0);
                level.addFreshEntity(activePervert);
                pervertActive = true;
                pervertReactionTimer = 600; // 30 Saniye izleme süresi
            }
        }

        if (pervertActive && activePervert != null) {
            pervertReactionTimer--;

            // Oyuncu Pervert'a Bakarsa (Bakış Açısı Kontrolü)
            Vec3 lookVec = player.getLookAngle();
            Vec3 toPervert = new Vec3(activePervert.getX() - player.getX(), activePervert.getY() - player.getY(), activePervert.getZ() - player.getZ()).normalize();

            if (lookVec.dot(toPervert) > 0.8D) { // Oyuncu tam Pervert'a baktı
                // Pervert anında arkasında belirir
                activePervert.moveTo(player.getX(), player.getY(), player.getZ() - 1, player.getYRot(), player.getXRot());
                player.sendSystemMessage(Component.literal("§4Behind You Look"));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 255, false, false));

                // 10 saniye sonra arkasını dönmezse yarım kalp bırakır
                player.getServer().execute(() -> {
                    try { Thread.sleep(10000); } catch (Exception ignored) {}
                    player.setHealth(1.0f);
                    player.sendSystemMessage(Component.literal("§5*Pervert korkunç bir kahkaha atarak kayboldu...*"));
                    if (activePervert != null) activePervert.discard();
                    pervertActive = false;
                });
            }

            // 30 saniye boyunca hiç bakmazsa sessizce silinir
            if (pervertReactionTimer <= 0) {
                activePervert.discard();
                pervertActive = false;
            }
        }
    }
}
