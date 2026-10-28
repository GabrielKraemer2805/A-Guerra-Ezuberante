package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.combat.Rachadinha;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;

/** One short-lived activation owns the path, damage budget and bounded surface destruction. */
public final class RachadinhaEntity extends Entity {
    private static final GameProfile TERRAIN_ACTOR=new GameProfile(UUID.fromString("b48f86a9-4094-4d47-b595-e27227f9940a"),"[GE:Rachadinha]");
    public static final int SEGMENT_TICKS=3,FADE_TICKS=24;
    private static final EntityDataAccessor<CompoundTag> PATH=SynchedEntityData.defineId(RachadinhaEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> REVEALED=SynchedEntityData.defineId(RachadinhaEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH=SynchedEntityData.defineId(RachadinhaEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(RachadinhaEntity.class,EntityDataSerializers.INT);
    private List<Rachadinha.Surface> path=List.of();
    private CompoundTag cachedPath;
    private UUID casterId,targetId;
    private final Set<UUID> hit=new HashSet<>();
    private final Set<BlockPos> broken=new HashSet<>();
    private float damage,knockback,finalRadius;
    private boolean blockDamage;
    public RachadinhaEntity(EntityType<? extends RachadinhaEntity> type,Level level){super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(){entityData.define(PATH,new CompoundTag());entityData.define(REVEALED,0);entityData.define(WIDTH,1F);entityData.define(AGE,0);}
    public void initialize(FlavioBolsonaroEntity caster,LivingEntity target,List<Rachadinha.Surface> path){
        this.path=path.stream().limit(Rachadinha.MAX_CELLS).toList();casterId=caster.getUUID();targetId=target.getUUID();setPos(caster.position());
        damage=ModConfig.FISSURE_DAMAGE.get().floatValue();knockback=ModConfig.FISSURE_KNOCKBACK.get().floatValue();blockDamage=ModConfig.FISSURE_BLOCK_DAMAGE.get();
        finalRadius=ModConfig.FISSURE_FINAL_RADIUS.get().floatValue();
        entityData.set(WIDTH,ModConfig.FISSURE_WIDTH.get().floatValue());
        var data=new CompoundTag();var list=new ListTag();
        for(var cell:this.path){var item=new CompoundTag();item.putLong("Block",cell.block().asLong());item.putDouble("X",cell.point().x);item.putDouble("Y",cell.point().y);item.putDouble("Z",cell.point().z);list.add(item);}
        data.put("Cells",list);entityData.set(PATH,data);
    }
    public List<Rachadinha.Surface> surfaces(){
        if(level().isClientSide && cachedPath!=entityData.get(PATH)){
            cachedPath=entityData.get(PATH);var cells=new ArrayList<Rachadinha.Surface>();
            for(Tag value:cachedPath.getList("Cells",Tag.TAG_COMPOUND)){
                var c=(CompoundTag)value;cells.add(new Rachadinha.Surface(BlockPos.of(c.getLong("Block")),new Vec3(c.getDouble("X"),c.getDouble("Y"),c.getDouble("Z"))));
                if(cells.size()==Rachadinha.MAX_CELLS)break;
            }
            path=List.copyOf(cells);
        }
        return path;
    }
    public int revealed(){return entityData.get(REVEALED);}
    public int effectAge(){return entityData.get(AGE);}
    public float fissureWidth(){return entityData.get(WIDTH);}
    public UUID casterId(){return casterId;}
    public int damagedEntities(){return hit.size();}
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        var server=(ServerLevel)level();
        if(!(server.getEntity(casterId) instanceof FlavioBolsonaroEntity caster) || !caster.isAlive() || path.isEmpty()){discard();return;}
        int age=effectAge();
        if(age%SEGMENT_TICKS==0 && revealed()<path.size()){
            var cell=path.get(revealed());
            if(!server.hasChunkAt(cell.block())){discard();return;}
            applySegment(server,caster,cell,revealed()==path.size()-1);entityData.set(REVEALED,revealed()+1);
        }
        entityData.set(AGE,age+1);
        if(age>=path.size()*SEGMENT_TICKS+FADE_TICKS)discard();
    }
    private void applySegment(ServerLevel level,FlavioBolsonaroEntity caster,Rachadinha.Surface cell,boolean last){
        var p=cell.point();var state=level.getBlockState(cell.block());
        if(!state.isAir())level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),p.x,p.y+.1,p.z,last?44:16,last?.7:.28,last?.4:.25,last?.7:.28,.16);
        level.sendParticles(ParticleTypes.POOF,p.x,p.y+.12,p.z,last?24:7,last?.9:.3,.2,last?.9:.3,.04);
        if(last){
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,p.x,p.y+.35,p.z,1,0,0,0,0);
            level.sendParticles(ParticleTypes.CLOUD,p.x,p.y+.1,p.z,28,finalRadius*.65,.08,finalRadius*.65,.08);
        }else level.sendParticles(ParticleTypes.EXPLOSION,p.x,p.y+.18,p.z,1,0,0,0,0);
        level.playSound(null,p.x,p.y,p.z,last?SoundEvents.GENERIC_EXPLODE:SoundEvents.STONE_BREAK,SoundSource.HOSTILE,last?1.35F:.7F,last?.85F:1.0F);
        // Line-of-sight and hostility are checked per actual impact, never on the client.
        double radius=last?finalRadius:.65+Math.max(0,(fissureWidth()-1)*.5);
        var box=new AABB(p.x-radius,p.y-.2,p.z-radius,p.x+radius,p.y+(last?2.6:2.1),p.z+radius);
        for(var victim:level.getEntitiesOfClass(LivingEntity.class,box,v->v.isAlive() && !hit.contains(v.getUUID()) && caster.isHostileForFissure(v,targetId))){
            double dx=Math.max(0,Math.max(victim.getBoundingBox().minX-p.x,p.x-victim.getBoundingBox().maxX));
            double dz=Math.max(0,Math.max(victim.getBoundingBox().minZ-p.z,p.z-victim.getBoundingBox().maxZ));
            if(last && dx*dx+dz*dz>radius*radius)continue;
            var from=p.add(0,.25,0);var to=victim.getBoundingBox().getCenter();
            if(level.clip(new net.minecraft.world.level.ClipContext(from,to,net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,caster)).getType()!=HitResult.Type.MISS)continue;
            hit.add(victim.getUUID());
            if(victim.hurt(level.damageSources().mobAttack(caster),damage))victim.knockback(knockback,caster.getX()-victim.getX(),caster.getZ()-victim.getZ());
        }
        if(blockDamage && ForgeEventFactory.getMobGriefingEvent(level,caster)){
            breakSurface(level,caster,cell.block());
            if(fissureWidth()>1.25F){
                var direction=p.subtract(caster.position());var side=new Vec3(direction.z,0,-direction.x).normalize();
                var adjacent=cell.block().offset((int)Math.round(side.x),0,(int)Math.round(side.z));
                breakSurface(level,caster,adjacent);
            }
        }
    }
    private void breakSurface(ServerLevel level,FlavioBolsonaroEntity caster,BlockPos pos){
        // Point-blank recasts must not dig the caster into an ever-deeper hole.
        if(pos.equals(caster.blockPosition().below()))return;
        if(!broken.add(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos))return;
        var state=level.getBlockState(pos);
        // Surface terrain only. Containers, protected/indestructible blocks and construction materials are not excavated.
        boolean terrain=state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.NETHERRACK) || state.is(Blocks.END_STONE);
        if(!terrain || state.hasBlockEntity() || state.getDestroySpeed(level,pos)<0 || !state.canEntityDestroy(level,pos,caster)
                || !ForgeEventFactory.onEntityDestroyBlock(caster,pos,state))return;
        if(!level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty())return;
        // Also offer the standard per-block break hook to claim/protection mods.
        // This actor is never OP and never borrows an author's player identity or inventory.
        var actor=FakePlayerFactory.get(level,TERRAIN_ACTOR);actor.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        actor.setPos(caster.position());
        if(!level.mayInteract(actor,pos) || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,actor)))return;
        level.destroyBlock(pos,false,caster);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<96*96;}
}
