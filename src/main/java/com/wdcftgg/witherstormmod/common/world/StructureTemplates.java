package com.wdcftgg.witherstormmod.common.world;

import com.google.common.base.Optional;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.resource.UpstreamResourceArchive;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Mirror;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.util.math.AxisAlignedBB;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Map;

public final class StructureTemplates {

    private static final Map<String, TemplateData> CACHE = new HashMap<String, TemplateData>();
    private static final Map<String, List<WeightedTemplate>> BOWELS_POOLS = createBowelsPools();
    private static final List<Block> FEATURE_CANNOT_REPLACE = Arrays.asList(
            Blocks.BEDROCK, Blocks.MOB_SPAWNER, Blocks.CHEST, Blocks.END_PORTAL_FRAME);
    private static final ITemplateProcessor FEATURE_PLACEMENT_PROCESSOR = new ITemplateProcessor() {
        @Override
        public Template.BlockInfo processBlock(World world, BlockPos worldPos, Template.BlockInfo blockInfo) {
            if (blockInfo.blockState.getBlock() == Blocks.AIR || cannotReplace(world, worldPos)) return null;
            return blockInfo;
        }
    };
    private static final ITemplateProcessor FEATURE_REMOVAL_PROCESSOR = new ITemplateProcessor() {
        @Override
        public Template.BlockInfo processBlock(World world, BlockPos worldPos, Template.BlockInfo blockInfo) {
            if (blockInfo.blockState.getBlock() == Blocks.AIR || cannotReplace(world, worldPos)) return null;
            return new Template.BlockInfo(blockInfo.pos, Blocks.AIR.getDefaultState(), null);
        }
    };

    private StructureTemplates() {
    }

    public static boolean place(World world, String id, BlockPos origin, Rotation rotation, boolean ignoreEntities) {
        Template template = get(id);
        if (template == null) return false;
        PlacementSettings settings = new PlacementSettings().setMirror(Mirror.NONE).setRotation(rotation)
                .setIgnoreEntities(ignoreEntities).setIgnoreStructureBlock(true);
        template.addBlocksToWorld(world, origin, FEATURE_PLACEMENT_PROCESSOR, settings, 2);
        return true;
    }

    /** 放置生成平台；自动生成模板返回数据标记，其余模板回退到结构原点。 */
    public static BlockPos placeStormSpawnPlatform(World world, String id, BlockPos origin, Rotation rotation) {
        TemplateData data = getData(id);
        if (data == null) return null;
        PlacementSettings settings = new PlacementSettings().setMirror(Mirror.NONE).setRotation(rotation)
                .setIgnoreEntities(false).setIgnoreStructureBlock(true);
        data.template.addBlocksToWorld(world, origin, FEATURE_PLACEMENT_PROCESSOR, settings, 2);
        for (DataMarker marker : data.dataMarkers) {
            if ("spawn_position".equals(marker.metadata)) {
                BlockPos spawnPosition = Template.transformedBlockPos(settings, marker.pos).add(origin);
                // Upstream Piece.handleDataMarker removes the structure block after recording it.
                world.setBlockToAir(spawnPosition);
                return spawnPosition;
            }
        }
        if ("auto_spawn_platform".equals(id)) {
            WitherStormMod.LOGGER.warn("Upstream automatic spawn platform has no spawn_position marker");
        }
        return origin;
    }

    public static boolean remove(World world, String id, BlockPos origin, Rotation rotation) {
        Template template = get(id);
        if (template == null) return false;
        PlacementSettings settings = new PlacementSettings().setMirror(Mirror.NONE).setRotation(rotation)
                .setIgnoreEntities(true).setIgnoreStructureBlock(true);
        template.addBlocksToWorld(world, origin, FEATURE_REMOVAL_PROCESSOR, settings, 2);
        return true;
    }

    /** 1.20 的 TemplateFeature 使用锚点坐标种子来选择结构旋转。 */
    public static Rotation getFeatureRotation(BlockPos anchor) {
        Rotation[] rotations = Rotation.values();
        return rotations[new Random(toModernBlockPosLong(anchor)).nextInt(rotations.length)];
    }

    static long toModernBlockPosLong(BlockPos pos) {
        return ((long) pos.getX() & 0x3FFFFFFL) << 38
                | ((long) pos.getZ() & 0x3FFFFFFL) << 12
                | (long) pos.getY() & 0xFFFL;
    }

    /** 返回使结构三轴中心与锚点重合的 1.12 模板原点。 */
    public static BlockPos getCenteredFeatureOrigin(Template template, BlockPos anchor, Rotation rotation) {
        RelativeBounds bounds = getRelativeBounds(template, rotation);
        return anchor.add(-bounds.centerX(), -bounds.centerY(), -bounds.centerZ());
    }

    /** Bowels podium 的锚点位于模板顶部上方一格，水平轴仍按结构中心对齐。 */
    public static BlockPos getTopAnchoredFeatureOrigin(Template template, BlockPos anchor, Rotation rotation) {
        RelativeBounds bounds = getRelativeBounds(template, rotation);
        return anchor.add(-bounds.centerX(), -template.getSize().getY(), -bounds.centerZ());
    }

    /** Upstream CommandBlockPodiumFeature centers the template on all three axes. */
    public static BlockPos getFeatureOrigin(BlockPos anchor, Template template, Rotation rotation) {
        RelativeBounds bounds = getRelativeBounds(template, rotation);
        return anchor.add(-bounds.centerX(), -bounds.centerY(), -bounds.centerZ());
    }

    public static Template get(String id) {
        TemplateData data = getData(id);
        return data == null ? null : data.template;
    }

    private static boolean cannotReplace(World world, BlockPos pos) {
        return FEATURE_CANNOT_REPLACE.contains(world.getBlockState(pos).getBlock());
    }

    private static RelativeBounds getRelativeBounds(Template template, Rotation rotation) {
        PlacementSettings settings = new PlacementSettings().setRotation(rotation);
        BlockPos size = template.getSize();
        BlockPos first = Template.transformedBlockPos(settings, BlockPos.ORIGIN);
        BlockPos last = Template.transformedBlockPos(settings,
                new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1));
        return new RelativeBounds(Math.min(first.getX(), last.getX()), Math.min(first.getY(), last.getY()),
                Math.min(first.getZ(), last.getZ()), Math.max(first.getX(), last.getX()),
                Math.max(first.getY(), last.getY()), Math.max(first.getZ(), last.getZ()));
    }

    public static boolean placeBowelsNetwork(World world, BlockPos center, Random random) {
        List<AxisAlignedBB> occupied = new ArrayList<AxisAlignedBB>();
        WeightedTemplate start = choose(BOWELS_POOLS.get("witherstormmod:bowels/bowels_mains"), random);
        if (start == null) return false;
        TemplateData main = getData(start.id);
        if (main == null) return false;
        // 上游 BowelsStructure 把起始块的底部对齐 start_height=100（原点 Y=100）；
        // 网络锚点 center 的 Y=96，因此垂直偏移为 +4。墙头凹槽、竞技场 Y=110 与
        // 升台高度 115/120/125 全部以此为基准。
        BlockPos origin = center.add(-main.template.getSize().getX() / 2, 4, -main.template.getSize().getZ() / 2);
        placePiece(world, main, origin, Rotation.NONE, occupied);
        List<PendingConnector> queue = new ArrayList<PendingConnector>();
        enqueue(queue, main, origin, Rotation.NONE, 0);
        int placed = 1;
        while (!queue.isEmpty() && placed < 48) {
            PendingConnector parent = queue.remove(0);
            if ("minecraft:empty".equals(parent.connector.pool)) continue;
            String selectedPool = parent.connector.pool;
            if (parent.depth >= 5 && "witherstormmod:bowels/bowels_caves".equals(selectedPool)) {
                selectedPool = "witherstormmod:bowels/bowels_caves_ends";
            } else if (parent.depth >= 5) {
                continue;
            }
            List<WeightedTemplate> pool = BOWELS_POOLS.get(selectedPool);
            if (pool == null || pool.isEmpty()) continue;
            List<WeightedTemplate> attempts = weightedOrder(pool, random);
            boolean attached = false;
            for (WeightedTemplate weighted : attempts) {
                TemplateData candidate = getData(weighted.id);
                if (candidate == null) continue;
                for (Connector child : candidate.connectors) {
                    if (!parent.connector.target.equals(child.name) || !child.target.equals(parent.connector.name)) continue;
                    for (Rotation rotation : Rotation.values()) {
                        EnumFacing childFacing = rotation.rotate(child.facing);
                        if (childFacing != parent.worldFacing.getOpposite()) continue;
                        BlockPos childPoint = Template.transformedBlockPos(
                                new PlacementSettings().setRotation(rotation), child.pos);
                        BlockPos targetPoint = parent.worldPos.offset(parent.worldFacing);
                        BlockPos childOrigin = targetPoint.subtract(childPoint);
                        AxisAlignedBB box = bounds(candidate.template, childOrigin, rotation);
                        if (intersectsAny(box, occupied)) continue;
                        placePiece(world, candidate, childOrigin, rotation, occupied);
                        enqueue(queue, candidate, childOrigin, rotation, parent.depth + 1);
                        placed++;
                        attached = true;
                        break;
                    }
                    if (attached) break;
                }
                if (attached) break;
            }
        }
        WitherStormMod.LOGGER.info("Placed Bowels template network with {} pieces", placed);
        return true;
    }

    private static TemplateData getData(String id) {
        synchronized (CACHE) {
            if (CACHE.containsKey(id)) return CACHE.get(id);
            TemplateData data = load(id);
            CACHE.put(id, data);
            return data;
        }
    }

    private static TemplateData load(String id) {
        String path = "data/witherstormmod/structures/" + id + ".nbt";
        try (InputStream stream = UpstreamResourceArchive.open(path)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(stream);
            List<Connector> connectors = readConnectors(root);
            List<DataMarker> dataMarkers = readDataMarkers(root);
            NBTTagList palette = root.getTagList("palette", 10);
            NBTTagList blocks = root.getTagList("blocks", 10);
            convertSkullBlockEntities(palette, blocks);
            convertPalette(palette);
            convertBlockEntities(blocks);
            root.setInteger("DataVersion", 1343);
            Template template = new Template();
            template.read(root);
            return new TemplateData(template, connectors, dataMarkers);
        } catch (IOException | RuntimeException exception) {
            WitherStormMod.LOGGER.error("Unable to convert upstream structure {}", id, exception);
            return null;
        }
    }

    private static void convertPalette(NBTTagList palette) {
        for (int index = 0; index < palette.tagCount(); index++) {
            NBTTagCompound modern = palette.getCompoundTagAt(index);
            IBlockState state = mapState(modern.getString("Name"), modern.getCompoundTag("Properties"));
            palette.set(index, NBTUtil.writeBlockState(new NBTTagCompound(), state));
        }
    }

    private static IBlockState mapState(String modernName, NBTTagCompound modernProperties) {
        Mapping mapping = mapName(modernName);
        String modernType = modernProperties.getString("type");
        if (modernName.startsWith("minecraft:") && modernName.endsWith("_slab") && "double".equals(modernType)) {
            if ("minecraft:wooden_slab".equals(mapping.name)) mapping = mapping.withName("minecraft:double_wooden_slab");
            else if ("minecraft:stone_slab".equals(mapping.name)) mapping = mapping.withName("minecraft:double_stone_slab");
            else if ("minecraft:stone_slab2".equals(mapping.name)) mapping = mapping.withName("minecraft:double_stone_slab2");
        }
        Block block = Block.REGISTRY.getObject(new ResourceLocation(mapping.name));
        if (block == null || block == Blocks.AIR && !"minecraft:air".equals(mapping.name)) block = Blocks.STONE;
        IBlockState state = block.getDefaultState();
        Map<String, String> properties = new HashMap<String, String>(mapping.properties);
        for (String key : modernProperties.getKeySet()) properties.put(key, modernProperties.getString(key));
        if (properties.containsKey("type") && block.getBlockState().getProperty("half") != null) {
            String type = properties.get("type");
            properties.put("half", "double".equals(type) ? "double" : type);
        }
        for (IProperty<?> property : block.getBlockState().getProperties()) {
            String value = properties.get(property.getName());
            if (value != null) state = apply(state, property, value);
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IBlockState apply(IBlockState state, IProperty property, String value) {
        Optional parsed = property.parseValue(value);
        return parsed.isPresent() ? state.withProperty(property, (Comparable) parsed.get()) : state;
    }

    private static Mapping mapName(String name) {
        if (name.startsWith("witherstormmod:")) return new Mapping(name);
        String path = name.substring(name.indexOf(':') + 1);
        if (path.equals("jigsaw") || path.equals("structure_block")) return new Mapping("minecraft:air");
        if (path.equals("wither_skeleton_skull"))
            return new Mapping("minecraft:skull", "facing", "up");
        if (path.equals("wither_skeleton_wall_skull")) return new Mapping("minecraft:skull");
        ResourceLocation directId = new ResourceLocation(name);
        if (Block.REGISTRY.containsKey(directId)) return new Mapping(name);
        ResourceLocation futureId = new ResourceLocation("futuremc", path);
        if (Block.REGISTRY.containsKey(futureId)) return new Mapping(futureId.toString());
        String[] woods = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
        for (int i = 0; i < woods.length; i++) {
            String wood = woods[i];
            if (path.equals(wood + "_planks")) return new Mapping("minecraft:planks", "variant", wood);
            if (path.equals(wood + "_slab")) return new Mapping("minecraft:wooden_slab", "variant", wood, "half", "bottom");
            if (path.equals(wood + "_button")) return new Mapping("minecraft:wooden_button");
            if (path.equals(wood + "_pressure_plate")) return new Mapping("minecraft:wooden_pressure_plate");
            if (path.equals(wood + "_wall_sign")) return new Mapping("minecraft:wall_sign");
            if (path.equals(wood + "_sign")) return new Mapping("minecraft:standing_sign");
            if (path.equals(wood + "_trapdoor")) return new Mapping("minecraft:trapdoor");
            if (path.equals(wood + "_log") || path.equals("stripped_" + wood + "_log") || path.equals(wood + "_wood")
                    || path.equals("stripped_" + wood + "_wood")) {
                return i < 4 ? new Mapping("minecraft:log", "variant", wood) : new Mapping("minecraft:log2", "variant", wood);
            }
        }
        if (path.equals("granite") || path.equals("diorite") || path.equals("andesite"))
            return new Mapping("minecraft:stone", "variant", path);
        if (path.equals("stone_slab") || path.equals("smooth_stone_slab")) return new Mapping("minecraft:stone_slab", "variant", "stone");
        if (path.equals("cobblestone_slab")) return new Mapping("minecraft:stone_slab", "variant", "cobblestone");
        if (path.equals("brick_slab")) return new Mapping("minecraft:stone_slab", "variant", "brick");
        if (path.equals("stone_brick_slab")) return new Mapping("minecraft:stone_slab", "variant", "stone_brick");
        if (path.equals("sandstone_slab")) return new Mapping("minecraft:stone_slab", "variant", "sandstone");
        if (path.equals("red_sandstone_slab")) return new Mapping("minecraft:stone_slab2", "variant", "red_sandstone");
        if (path.equals("purpur_slab")) return new Mapping("minecraft:purpur_slab");
        if (path.equals("stone_bricks")) return new Mapping("minecraft:stonebrick", "variant", "stonebrick");
        if (path.equals("cracked_stone_bricks")) return new Mapping("minecraft:stonebrick", "variant", "cracked_stonebrick");
        if (path.equals("chiseled_stone_bricks") || path.equals("infested_chiseled_stone_bricks"))
            return new Mapping("minecraft:stonebrick", "variant", "chiseled_stonebrick");
        if (path.equals("bricks")) return new Mapping("minecraft:brick_block");
        if (path.equals("cobweb")) return new Mapping("minecraft:web");
        if (path.equals("grass_block")) return new Mapping("minecraft:grass");
        if (path.equals("dirt_path")) return new Mapping("minecraft:grass_path");
        if (path.equals("wall_torch")) return new Mapping("minecraft:torch");
        if (path.equals("water_cauldron")) return new Mapping("minecraft:cauldron");
        if (path.equals("barrel")) return new Mapping("minecraft:chest");
        if (path.equals("blast_furnace") || path.equals("smoker")) return new Mapping("minecraft:furnace");
        if (path.equals("campfire")) return new Mapping("minecraft:netherrack");
        if (path.equals("beehive")) return new Mapping("minecraft:log");
        if (path.contains("deepslate") || path.equals("tuff") || path.equals("blackstone") || path.equals("dripstone_block"))
            return new Mapping("minecraft:stonebrick");
        if (path.contains("candle") || path.equals("lantern") || path.equals("soul_lantern")) return new Mapping("minecraft:torch");
        if (path.startsWith("potted_")) return new Mapping("minecraft:flower_pot");
        if (path.equals("short_grass")) return new Mapping("minecraft:tallgrass", "type", "grass");
        if (path.equals("tall_grass")) return new Mapping("minecraft:double_plant", "variant", "double_grass");
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        for (String color : colors) {
            if (path.equals(color + "_wool")) return new Mapping("minecraft:wool", "color", color);
            if (path.equals(color + "_stained_glass")) return new Mapping("minecraft:stained_glass", "color", color);
            if (path.equals(color + "_stained_glass_pane")) return new Mapping("minecraft:stained_glass_pane", "color", color);
            if (path.equals(color + "_terracotta")) return new Mapping("minecraft:stained_hardened_clay", "color", color);
            if (path.equals(color + "_concrete")) return new Mapping("minecraft:concrete", "color", color);
            if (path.equals(color + "_carpet")) return new Mapping("minecraft:carpet", "color", color);
        }
        return new Mapping("minecraft:stone");
    }

    private static void convertSkullBlockEntities(NBTTagList palette, NBTTagList blocks) {
        for (int index = 0; index < blocks.tagCount(); index++) {
            NBTTagCompound block = blocks.getCompoundTagAt(index);
            int stateIndex = block.getInteger("state");
            if (stateIndex < 0 || stateIndex >= palette.tagCount()) continue;
            NBTTagCompound modernState = palette.getCompoundTagAt(stateIndex);
            String name = modernState.getString("Name");
            boolean standing = "minecraft:wither_skeleton_skull".equals(name);
            if (!standing && !"minecraft:wither_skeleton_wall_skull".equals(name)) continue;

            NBTTagCompound data = block.hasKey("nbt", 10)
                    ? block.getCompoundTag("nbt") : new NBTTagCompound();
            data.setString("id", "minecraft:skull");
            data.setByte("SkullType", (byte) 1);
            if (standing) {
                String rotation = modernState.getCompoundTag("Properties").getString("rotation");
                try {
                    data.setByte("Rot", (byte) (Integer.parseInt(rotation) & 15));
                } catch (NumberFormatException ignored) {
                    data.setByte("Rot", (byte) 0);
                }
            }
            block.setTag("nbt", data);
        }
    }

    private static void convertBlockEntities(NBTTagList blocks) {
        for (int index = 0; index < blocks.tagCount(); index++) {
            NBTTagCompound data = blocks.getCompoundTagAt(index).getCompoundTag("nbt");
            if (!data.hasKey("id", 8)) continue;
            String id = data.getString("id");
            if ("minecraft:barrel".equals(id) || "minecraft:blast_furnace".equals(id)
                    || "minecraft:smoker".equals(id) || "minecraft:beehive".equals(id)
                    || "minecraft:bee_nest".equals(id) || "minecraft:bell".equals(id)
                    || "minecraft:campfire".equals(id)) {
                data.setString("id", "futuremc:" + id.substring("minecraft:".length()));
                if ("minecraft:bee_nest".equals(id)) data.setString("id", "futuremc:beehive");
            } else if (id.startsWith("minecraft:") && (id.contains("jigsaw")
                    || id.contains("lectern") || id.contains("sculk"))) data.removeTag("id");
            if (data.hasKey("LootTable", 8) && data.getString("LootTable").equals("witherstormmod:chests/bowels_general"))
                data.setString("LootTable", "witherstormmod:chests/bowels_general");
        }
    }

    private static List<Connector> readConnectors(NBTTagCompound root) {
        List<Connector> result = new ArrayList<Connector>();
        NBTTagList palette = root.getTagList("palette", 10);
        NBTTagList blocks = root.getTagList("blocks", 10);
        for (int index = 0; index < blocks.tagCount(); index++) {
            NBTTagCompound block = blocks.getCompoundTagAt(index);
            NBTTagCompound state = palette.getCompoundTagAt(block.getInteger("state"));
            if (!"minecraft:jigsaw".equals(state.getString("Name"))) continue;
            NBTTagList position = block.getTagList("pos", 3);
            NBTTagCompound data = block.getCompoundTag("nbt");
            String orientation = state.getCompoundTag("Properties").getString("orientation");
            String facingName = orientation.contains("_") ? orientation.substring(0, orientation.indexOf('_')) : orientation;
            EnumFacing facing = EnumFacing.byName(facingName);
            if (facing == null) facing = EnumFacing.NORTH;
            result.add(new Connector(new BlockPos(position.getIntAt(0), position.getIntAt(1), position.getIntAt(2)),
                    facing, data.getString("name"), data.getString("target"), data.getString("pool")));
        }
        return result;
    }

    private static List<DataMarker> readDataMarkers(NBTTagCompound root) {
        List<DataMarker> result = new ArrayList<DataMarker>();
        NBTTagList palette = root.getTagList("palette", 10);
        NBTTagList blocks = root.getTagList("blocks", 10);
        for (int index = 0; index < blocks.tagCount(); index++) {
            NBTTagCompound block = blocks.getCompoundTagAt(index);
            int stateIndex = block.getInteger("state");
            if (stateIndex < 0 || stateIndex >= palette.tagCount()
                    || !"minecraft:structure_block".equals(palette.getCompoundTagAt(stateIndex).getString("Name"))) {
                continue;
            }
            NBTTagCompound data = block.getCompoundTag("nbt");
            String metadata = data.getString("metadata");
            if (metadata.isEmpty()) continue;
            NBTTagList position = block.getTagList("pos", 3);
            result.add(new DataMarker(new BlockPos(position.getIntAt(0), position.getIntAt(1),
                    position.getIntAt(2)), metadata));
        }
        return result;
    }

    private static void enqueue(List<PendingConnector> queue, TemplateData data, BlockPos origin, Rotation rotation, int depth) {
        PlacementSettings settings = new PlacementSettings().setRotation(rotation);
        for (Connector connector : data.connectors) {
            BlockPos transformed = Template.transformedBlockPos(settings, connector.pos).add(origin);
            queue.add(new PendingConnector(connector, transformed, rotation.rotate(connector.facing), depth));
        }
    }

    private static void placePiece(World world, TemplateData data, BlockPos origin, Rotation rotation, List<AxisAlignedBB> occupied) {
        data.template.addBlocksToWorld(world, origin, new PlacementSettings().setRotation(rotation)
                .setIgnoreEntities(false).setIgnoreStructureBlock(true), 2);
        occupied.add(bounds(data.template, origin, rotation));
    }

    private static AxisAlignedBB bounds(Template template, BlockPos origin, Rotation rotation) {
        BlockPos size = template.getSize();
        PlacementSettings settings = new PlacementSettings().setRotation(rotation);
        BlockPos a = Template.transformedBlockPos(settings, BlockPos.ORIGIN).add(origin);
        BlockPos b = Template.transformedBlockPos(settings, new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1)).add(origin);
        return new AxisAlignedBB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    private static boolean intersectsAny(AxisAlignedBB box, List<AxisAlignedBB> occupied) {
        AxisAlignedBB inset = box.grow(-1.0D);
        for (AxisAlignedBB other : occupied) if (inset.intersects(other.grow(-1.0D))) return true;
        return false;
    }

    private static WeightedTemplate choose(List<WeightedTemplate> values, Random random) {
        if (values == null || values.isEmpty()) return null;
        int total = 0;
        for (WeightedTemplate value : values) total += value.weight;
        int selected = random.nextInt(total);
        for (WeightedTemplate value : values) {
            selected -= value.weight;
            if (selected < 0) return value;
        }
        return values.get(values.size() - 1);
    }

    private static List<WeightedTemplate> weightedOrder(List<WeightedTemplate> values, Random random) {
        List<WeightedTemplate> remaining = new ArrayList<WeightedTemplate>(values);
        List<WeightedTemplate> ordered = new ArrayList<WeightedTemplate>();
        while (!remaining.isEmpty()) {
            WeightedTemplate selected = choose(remaining, random);
            ordered.add(selected);
            remaining.remove(selected);
        }
        return ordered;
    }

    private static Map<String, List<WeightedTemplate>> createBowelsPools() {
        Map<String, List<WeightedTemplate>> pools = new HashMap<String, List<WeightedTemplate>>();
        pools.put("witherstormmod:bowels/bowels_mains", weights("bowels/bowels_main", 5, "bowels/bowels_main_pillars", 5, "bowels/bowels_main_shafts", 5));
        pools.put("witherstormmod:bowels/bowels_branch_tunnels", weights("bowels/bowels_tunnel_branching", 1));
        pools.put("witherstormmod:bowels/bowels_chutes", weights("bowels/bowels_drop", 1));
        pools.put("witherstormmod:bowels/bowels_tunnels", weights("bowels/bowels_tunnel_obstructed", 10, "bowels/bowels_tunnel", 10,
                "bowels/bowels_tunnel_garden", 10, "bowels/bowels_tunnel_mineshaft", 9, "bowels/bowels_tunnel_village", 9));
        pools.put("witherstormmod:bowels/bowels_tunnels_caves", weights("bowels/bowels_tunnel_obstructed_caves", 10, "bowels/bowels_tunnel_caves", 10,
                "bowels/bowels_tunnel_garden_caves", 10, "bowels/bowels_tunnel_mineshaft_caves", 10, "bowels/bowels_tunnel_village_caves", 10));
        pools.put("witherstormmod:bowels/bowels_caves", weights("bowels/bowels_cave_4way", 6, "bowels/bowels_cave_chute", 6,
                "bowels/bowels_cave_long", 6, "bowels/bowels_cave_tainted_trap", 2, "bowels/bowels_cave_forgotten_memories", 2,
                "bowels/bowels_cave_deep_dark_maze", 3, "bowels/bowels_cave_crippled_castle", 2, "bowels/bowels_cave_blockage", 2,
                "bowels/bowels_cave_bone_hallway", 3, "bowels/bowels_cave_castle_dinner_room", 4, "bowels/bowels_cave_party_crashed", 4,
                "bowels/bowels_cave_tainted_depths", 2, "bowels/bowels_cave_tainted_drip", 3, "bowels/bowels_cave_tainted_hut", 4,
                "bowels/bowels_cave_wither_ruins", 4));
        pools.put("witherstormmod:bowels/bowels_caves_ends", weights("bowels/bowels_cave_small_end", 10, "bowels/bowels_cave_portal_end", 8,
                "bowels/bowels_cave_nostalgia", 6, "bowels/bowels_cave_tree_end", 6, "bowels/bowels_cave_outpost", 10,
                "bowels/bowels_cave_pyramid", 10, "bowels/bowels_cave_fragmented_fortress", 10, "bowels/bowels_cave_ruined_portal", 8,
                "bowels/bowels_cave_ruined_treasure", 6, "bowels/bowels_cave_twisted_village", 8,
                "bowels/bowels_cave_long_lost_volcano", 6, "bowels/bowels_cave_origin", 6, "bowels/bowels_cave_overgrown_tainted_tree", 6,
                "bowels/bowels_cave_pama_machine", 4, "bowels/bowels_cave_statues_of_order", 4, "bowels/bowels_cave_tainted_beach", 6,
                "bowels/bowels_cave_tainted_redstone", 4, "bowels/bowels_cave_tainted_terminal", 2, "bowels/bowels_cave_trapped_chest", 4,
                "bowels/bowels_cave_weathered_street_path", 6, "bowels/bowels_cave_withered_well", 2, "bowels/bowels_cave_woodland_mansion", 4,
                "bowels/bowels_cave_temple_of_an_old_warrior", 4));
        return pools;
    }

    private static List<WeightedTemplate> weights(Object... values) {
        List<WeightedTemplate> result = new ArrayList<WeightedTemplate>();
        for (int index = 0; index + 1 < values.length; index += 2)
            result.add(new WeightedTemplate((String) values[index], (Integer) values[index + 1]));
        return result;
    }

    private static final class TemplateData {
        private final Template template;
        private final List<Connector> connectors;
        private final List<DataMarker> dataMarkers;

        private TemplateData(Template template, List<Connector> connectors, List<DataMarker> dataMarkers) {
            this.template = template;
            this.connectors = connectors;
            this.dataMarkers = dataMarkers;
        }
    }

    private static final class DataMarker {
        private final BlockPos pos;
        private final String metadata;

        private DataMarker(BlockPos pos, String metadata) {
            this.pos = pos;
            this.metadata = metadata;
        }
    }

    private static final class Connector {
        private final BlockPos pos; private final EnumFacing facing; private final String name; private final String target; private final String pool;
        private Connector(BlockPos pos, EnumFacing facing, String name, String target, String pool) {
            this.pos = pos; this.facing = facing; this.name = name; this.target = target; this.pool = pool;
        }
    }

    private static final class PendingConnector {
        private final Connector connector; private final BlockPos worldPos; private final EnumFacing worldFacing; private final int depth;
        private PendingConnector(Connector connector, BlockPos worldPos, EnumFacing worldFacing, int depth) {
            this.connector = connector; this.worldPos = worldPos; this.worldFacing = worldFacing; this.depth = depth;
        }
    }

    private static final class RelativeBounds {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;

        private RelativeBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        private int centerX() { return minX + (maxX - minX + 1) / 2; }
        private int centerY() { return minY + (maxY - minY + 1) / 2; }
        private int centerZ() { return minZ + (maxZ - minZ + 1) / 2; }
    }

    private static final class WeightedTemplate {
        private final String id; private final int weight;
        private WeightedTemplate(String id, int weight) { this.id = id; this.weight = weight; }
    }

    private static final class Mapping {
        private final String name;
        private final Map<String, String> properties = new HashMap<String, String>();

        private Mapping(String name, String... values) {
            this.name = name;
            for (int index = 0; index + 1 < values.length; index += 2) properties.put(values[index], values[index + 1]);
        }

        private Mapping withName(String replacement) {
            Mapping mapping = new Mapping(replacement);
            mapping.properties.putAll(properties);
            return mapping;
        }
    }
}
