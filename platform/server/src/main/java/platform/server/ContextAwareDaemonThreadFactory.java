package platform.server;

import platform.interop.DaemonThreadFactory;

public class ContextAwareDaemonThreadFactory extends DaemonThreadFactory {
    private final Context context;

    public ContextAwareDaemonThreadFactory(Context context) {
        this(context, "-context-aware-daemon-");

    }

    public ContextAwareDaemonThreadFactory(Context context, String threadNamePrefix) {
        super(threadNamePrefix);
        this.context = context;
    }

    protected Thread newThreadInstance(ThreadGroup group, Runnable r, String name, int stackSize) {
        return new ContextAwareThread(context, group, r, name, stackSize);
    }
}
