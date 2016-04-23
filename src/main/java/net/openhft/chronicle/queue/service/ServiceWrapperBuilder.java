package net.openhft.chronicle.queue.service;

import net.openhft.chronicle.core.threads.EventLoop;
import net.openhft.chronicle.core.threads.HandlerPriority;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.threads.EventGroup;
import net.openhft.chronicle.wire.MethodReader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by peter on 23/04/16.
 */
public class ServiceWrapperBuilder<O> implements Supplier<ServiceWrapper> {
    private List<String> inputPaths = new ArrayList<>();
    private String outputPath;
    private Class<O> outClass;
    private List<Function<O, Object>> serviceFunctions = new ArrayList<>();
    private EventLoop eventLoop;
    private HandlerPriority priority = HandlerPriority.MEDIUM;
    private boolean createdEventLoop = false;
    private int inputSourceId;
    private int outputSourceId;

    ServiceWrapperBuilder() {
    }

    public static <O> ServiceWrapperBuilder<O> serviceBuilder(String inputPath, String outputPath, Class<O> outClass, Function<O, Object> serviceFunction) {
        ServiceWrapperBuilder<O> swb = new ServiceWrapperBuilder<>();
        swb.addInputPath(inputPath);
        swb.outputPath = outputPath;
        swb.outClass = outClass;
        swb.addServiceFunction(serviceFunction);
        return swb;
    }

    public List<String> inputPath() {
        return inputPaths;
    }

    public ServiceWrapperBuilder<O> addInputPath(String inputPath) {
        this.inputPaths.add(inputPath);
        return this;
    }

    public Class<O> outClass() {
        return outClass;
    }

    public ServiceWrapperBuilder<O> outClass(Class<O> outClass) {
        this.outClass = outClass;
        return this;
    }

    public String outputPath() {
        return outputPath;
    }

    public ServiceWrapperBuilder<O> outputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }

    public List<Function<O, Object>> getServiceFunctions() {
        return serviceFunctions;
    }

    public ServiceWrapperBuilder<O> addServiceFunction(Function<O, Object> serviceFunctions) {
        this.serviceFunctions.add(serviceFunctions);
        return this;
    }

    public EventLoop eventLoop() {
        return eventLoop;
    }

    public boolean createdEventLoop() {
        return createdEventLoop;
    }

    public void eventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public HandlerPriority priority() {
        return priority;
    }

    public ServiceWrapperBuilder<O> priority(HandlerPriority priority) {
        this.priority = priority;
        return this;
    }

    public int inputSourceId() {
        return inputSourceId;
    }

    public ServiceWrapperBuilder<O> inputSourceId(int inputSourceId) {
        this.inputSourceId = inputSourceId;
        return this;
    }

    public int outputSourceId() {
        return outputSourceId;
    }

    public ServiceWrapperBuilder<O> outputSourceId(int outputSourceId) {
        this.outputSourceId = outputSourceId;
        return this;
    }

    @Override
    public ServiceWrapper get() {
        if (eventLoop == null) {
            eventLoop = new EventGroup(false);
            createdEventLoop = true;
        }
        return new EventLoopServiceWrapper<>(this);
    }

    public SingleChronicleQueue inputQueue() {
        return SingleChronicleQueueBuilder.binary(inputPaths.get(0)).sourceId(inputSourceId()).build();
    }

    public SingleChronicleQueue outputQueue() {
        return SingleChronicleQueueBuilder.binary(outputPath).sourceId(outputSourceId()).build();
    }

    public MethodReader outputReader(Object... impls) {
        SingleChronicleQueue queue = outputQueue();
        MethodReader reader = queue.createTailer().methodReader(impls);
        reader.closeIn(true);
        return reader;
    }

    public <T> T inputWriter(Class<T> tClass) {
        SingleChronicleQueue queue = inputQueue();
        return queue.createAppender().methodWriterBuilder(tClass).recordHistory(true).onClose(queue).get();
    }
}
