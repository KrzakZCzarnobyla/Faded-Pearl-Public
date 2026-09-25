package pl.fadedpearl.world;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Bounded survey of the local passable cave volume; uncertainty rejects a new encounter. */
public final class CaveConnectivity {
    public enum Result { SAME, DISTINCT, INDETERMINATE }
    public static final int LOCAL_HORIZONTAL_RANGE = 192;
    private static final int MARGIN = 12;
    private static final int VERTICAL_MARGIN = 24;
    private static final int MAX_VISITED = 8_000;

    public static Result survey(BlockPos start, BlockPos target,
                                Predicate<BlockPos> loaded, Predicate<BlockPos> passage) {
        if (Math.abs(start.getX() - target.getX()) > LOCAL_HORIZONTAL_RANGE
                || Math.abs(start.getZ() - target.getZ()) > LOCAL_HORIZONTAL_RANGE
                || Math.abs(start.getY() - target.getY()) > 64) return Result.DISTINCT;
        if (!loaded.test(start) || !loaded.test(target)) return Result.INDETERMINATE;
        if (!passage.test(start) || !passage.test(target)) return Result.INDETERMINATE;
        if (start.equals(target)) return Result.SAME;

        int minX = Math.min(start.getX(), target.getX()) - MARGIN;
        int maxX = Math.max(start.getX(), target.getX()) + MARGIN;
        int minY = Math.min(start.getY(), target.getY()) - VERTICAL_MARGIN;
        int maxY = Math.max(start.getY(), target.getY()) + VERTICAL_MARGIN;
        int minZ = Math.min(start.getZ(), target.getZ()) - MARGIN;
        int maxZ = Math.max(start.getZ(), target.getZ()) + MARGIN;
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        open.add(start);
        visited.add(start.asLong());
        boolean touchedUnloaded = false;
        while (!open.isEmpty()) {
            BlockPos current = open.removeFirst();
            for (BlockPos next : new BlockPos[]{current.north(), current.south(), current.east(),
                    current.west(), current.above(), current.below()}) {
                if (next.getX() < minX || next.getX() > maxX || next.getY() < minY || next.getY() > maxY
                        || next.getZ() < minZ || next.getZ() > maxZ || visited.contains(next.asLong())) continue;
                if (!loaded.test(next)) { touchedUnloaded = true; continue; }
                if (!passage.test(next)) continue;
                if (next.equals(target)) return Result.SAME;
                visited.add(next.asLong());
                if (visited.size() >= MAX_VISITED) return Result.INDETERMINATE;
                open.addLast(next);
            }
        }
        return touchedUnloaded ? Result.INDETERMINATE : Result.DISTINCT;
    }

    private CaveConnectivity() {}
}
