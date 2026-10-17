package com.xreatlabs.xdiscordultimate.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Scheduler bridge that keeps the plugin working on Folia.
 * Falls back to the regular Bukkit scheduler everywhere else.
 */
public final class SchedulerCompat {

    private static final boolean FOLIA = detectFolia();

    private static Method globalSchedulerMethod;
    private static Method asyncSchedulerMethod;

    private SchedulerCompat() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static BukkitTask runTask(Plugin plugin, Runnable task) {
        if (!FOLIA) {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
        return foliaSync(plugin, task, 0L, -1L);
    }

    public static BukkitTask runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
        return foliaSync(plugin, task, delayTicks, -1L);
    }

    public static BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
        return foliaSync(plugin, task, delayTicks, periodTicks);
    }

    public static BukkitTask runTaskAsync(Plugin plugin, Runnable task) {
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
        return foliaAsync(plugin, task, -1L, -1L);
    }

    public static BukkitTask runTaskLaterAsync(Plugin plugin, Runnable task, long delayTicks) {
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
        return foliaAsync(plugin, task, delayTicks, -1L);
    }

    public static BukkitTask runTaskTimerAsync(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (!FOLIA) {
            return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
        return foliaAsync(plugin, task, delayTicks, periodTicks);
    }

    public static void cancelTasks(Plugin plugin) {
        if (!FOLIA) {
            Bukkit.getScheduler().cancelTasks(plugin);
            return;
        }
        try {
            invokeScheduler(globalSchedulerMethod(), "cancelTasks", new Class<?>[]{Plugin.class}, plugin);
            invokeScheduler(asyncSchedulerMethod(), "cancelTasks", new Class<?>[]{Plugin.class}, plugin);
        } catch (Throwable ignored) {
        }
    }

    private static BukkitTask foliaSync(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        try {
            Object scheduler = schedulerInstance(globalSchedulerMethod());
            Consumer<Object> consumer = ignored -> task.run();
            Object scheduled;
            if (periodTicks > 0) {
                scheduled = invoke(scheduler, "runAtFixedRate",
                    new Class<?>[]{Plugin.class, Consumer.class, long.class, long.class},
                    plugin, consumer, delayTicks, periodTicks);
            } else if (delayTicks > 0) {
                scheduled = invoke(scheduler, "runDelayed",
                    new Class<?>[]{Plugin.class, Consumer.class, long.class},
                    plugin, consumer, delayTicks);
            } else {
                scheduled = invoke(scheduler, "run",
                    new Class<?>[]{Plugin.class, Consumer.class},
                    plugin, consumer);
            }
            return new FoliaTask(plugin, scheduled, true);
        } catch (Throwable e) {
            return new FoliaTask(plugin, null, true);
        }
    }

    private static BukkitTask foliaAsync(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        try {
            Object scheduler = schedulerInstance(asyncSchedulerMethod());
            Consumer<Object> consumer = ignored -> task.run();
            Object scheduled;
            if (periodTicks > 0) {
                scheduled = invoke(scheduler, "runAtFixedRate",
                    new Class<?>[]{Plugin.class, Consumer.class, long.class, long.class, java.util.concurrent.TimeUnit.class},
                    plugin, consumer, delayTicks * 50L, periodTicks * 50L, java.util.concurrent.TimeUnit.MILLISECONDS);
            } else if (delayTicks > 0) {
                scheduled = invoke(scheduler, "runDelayed",
                    new Class<?>[]{Plugin.class, Consumer.class, long.class, java.util.concurrent.TimeUnit.class},
                    plugin, consumer, delayTicks * 50L, java.util.concurrent.TimeUnit.MILLISECONDS);
            } else {
                scheduled = invoke(scheduler, "runNow",
                    new Class<?>[]{Plugin.class, Consumer.class},
                    plugin, consumer);
            }
            return new FoliaTask(plugin, scheduled, false);
        } catch (Throwable e) {
            return new FoliaTask(plugin, null, false);
        }
    }

    private static Method globalSchedulerMethod() throws NoSuchMethodException {
        if (globalSchedulerMethod == null) {
            globalSchedulerMethod = Bukkit.class.getMethod("getGlobalRegionScheduler");
        }
        return globalSchedulerMethod;
    }

    private static Method asyncSchedulerMethod() throws NoSuchMethodException {
        if (asyncSchedulerMethod == null) {
            asyncSchedulerMethod = Bukkit.class.getMethod("getAsyncScheduler");
        }
        return asyncSchedulerMethod;
    }

    private static Object schedulerInstance(Method getter) throws Exception {
        return getter.invoke(null);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        return target.getClass().getMethod(name, types).invoke(target, args);
    }

    private static Object invokeScheduler(Method getter, String name, Class<?>[] types, Object... args) throws Exception {
        return invoke(schedulerInstance(getter), name, types, args);
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static final class FoliaTask implements BukkitTask {
        private final Plugin owner;
        private final Object scheduled;
        private final boolean sync;
        private volatile boolean cancelled;

        FoliaTask(Plugin owner, Object scheduled, boolean sync) {
            this.owner = owner;
            this.scheduled = scheduled;
            this.sync = sync;
        }

        @Override
        public int getTaskId() {
            return -1;
        }

        @Override
        public Plugin getOwner() {
            return owner;
        }

        @Override
        public boolean isSync() {
            return sync;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void cancel() {
            cancelled = true;
            if (scheduled == null) return;
            try {
                scheduled.getClass().getMethod("cancel").invoke(scheduled);
            } catch (Throwable ignored) {
            }
        }
    }
}
