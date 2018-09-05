package space.npstr.baymax;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by napster on 05.09.18.
 * <p>
 * Just like the JDA-Utils EventWaiter (Apache v2) but a lot less crappy aka
 * - threadsafe
 * - doesn't block the main JDA threads
 * - efficient
 * - stricter types
 * - doesn't have to wait 6 months (and counting) for fixes
 */
@Component
public class EventWaiter implements EventListener {

    //this thread pool runs the actions as well as the timeout actions
    private final ScheduledExecutorService pool;
    //modifications to the hash map and sets have to go through this single threaded pool
    private final ScheduledExecutorService single;

    //These stateful collections are only threadsafe when modified though the single executor
    private final List<WaitingEvent<? extends Event>> toRemove = new ArrayList<>();
    private final HashMap<Class<? extends Event>, Set<EventWaiter.WaitingEvent<? extends Event>>> waitingEvents;

    public EventWaiter(ScheduledThreadPoolExecutor jdaThreadPool) {
        this.waitingEvents = new HashMap<>();
        this.pool = jdaThreadPool;
        this.single = new ScheduledThreadPoolExecutor(1);
    }

    public <T extends Event> void waitForEvent(Class<T> classType, Predicate<T> condition,
                                               Consumer<T> action, long timeout, TimeUnit unit, Runnable timeoutAction) {
        EventWaiter.WaitingEvent<T> we = new EventWaiter.WaitingEvent<>(condition, action);

        this.single.execute(() -> {
            Set<EventWaiter.WaitingEvent<? extends Event>> set
                    = this.waitingEvents.computeIfAbsent(classType, c -> new HashSet<>());
            set.add(we);
            this.single.schedule(() -> {
                if (set.remove(we)) {
                    this.pool.execute(timeoutAction);
                }

                if (set.isEmpty()) {
                    this.waitingEvents.remove(classType);
                }
            }, timeout, unit);
        });
    }

    @Override
    public final void onEvent(Event event) {
        Class cc = event.getClass();

        // Runs at least once for the fired Event, at most
        // once for each superclass (excluding Object) because
        // Class#getSuperclass() returns null when the superclass
        // is primitive, void, or (in this case) Object.
        while (cc != null && cc != Object.class) {
            Class clazz = cc;
            if (this.waitingEvents.get(clazz) != null) {
                this.single.execute(() -> {
                    Set<WaitingEvent<? extends Event>> set = this.waitingEvents.get(clazz);
                    @SuppressWarnings("unchecked") Predicate<WaitingEvent> filter = we -> we.attempt(event);
                    set.stream().filter(filter).forEach(this.toRemove::add);
                    set.removeAll(this.toRemove);
                    this.toRemove.clear();

                    if (set.isEmpty()) {
                        this.waitingEvents.remove(clazz);
                    }
                });
            }

            cc = cc.getSuperclass();
        }
    }

    private class WaitingEvent<T extends Event> {
        final Predicate<T> condition;
        final Consumer<T> action;

        WaitingEvent(Predicate<T> condition, Consumer<T> action) {
            this.condition = condition;
            this.action = action;
        }

        boolean attempt(T event) {
            if (this.condition.test(event)) {
                EventWaiter.this.pool.execute(() -> this.action.accept(event));
                return true;
            }
            return false;
        }
    }

}
