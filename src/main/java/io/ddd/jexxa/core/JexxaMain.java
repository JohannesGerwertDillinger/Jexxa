package io.ddd.jexxa.core;

import java.lang.annotation.Annotation;
import java.util.Properties;

import io.ddd.jexxa.core.factory.ClassFactory;
import io.ddd.jexxa.core.factory.DrivenAdapterFactory;
import io.ddd.jexxa.core.factory.DrivingAdapterFactory;
import io.ddd.jexxa.core.factory.PortFactory;
import io.ddd.jexxa.infrastructure.drivingadapter.CompositeDrivingAdapter;
import io.ddd.jexxa.infrastructure.drivingadapter.IDrivingAdapter;
import io.ddd.jexxa.utils.JexxaLogger;
import org.apache.commons.lang.Validate;

public class JexxaMain
{
    private CompositeDrivingAdapter compositeDrivingAdapter;
    private Properties properties = new Properties();

    private DrivingAdapterFactory drivingAdapterFactory;
    private DrivenAdapterFactory drivenAdapterFactory;
    private PortFactory portFactory;

    private BoundedContext boundedContext;

    public JexxaMain(String contextName)
    {
        this(contextName, System.getProperties());
    }

    public JexxaMain(String contextName, Properties properties)
    {
        Validate.notNull(properties);
        Validate.notNull(contextName);

        this.boundedContext = new BoundedContext(contextName);
        this.properties.putAll( properties );
        this.properties.put("io.ddd.jexxa.context.name", contextName);

        this.compositeDrivingAdapter = new CompositeDrivingAdapter();

        this.drivingAdapterFactory = new DrivingAdapterFactory();
        this.drivenAdapterFactory = new DrivenAdapterFactory();
        this.portFactory = new PortFactory(drivenAdapterFactory);
    }

    public JexxaMain whiteListDrivenAdapterPackage(String packageName)
    {
        drivenAdapterFactory.whiteListPackage(packageName);
        return this;
    }

    public JexxaMain whiteListPortPackage(String packageName)
    {
        portFactory.whiteListPackage(packageName);
        return this;
    }

    public JexxaMain whiteListPackage(String packageName)
    {
        whiteListDrivenAdapterPackage(packageName);
        whiteListPortPackage(packageName);
        return this;
    }


    public void bindConfigAdapter(Class<? extends IDrivingAdapter> adapter) {
        Validate.notNull(adapter);

        var drivingAdapter = drivingAdapterFactory.getInstanceOf(adapter, properties);

        compositeDrivingAdapter.add(drivingAdapter);
    }

    public void bind(Class<? extends IDrivingAdapter> adapter, Class<?> port) {
        Validate.notNull(adapter);
        Validate.notNull(port);

        var drivingAdapter = drivingAdapterFactory.getInstanceOf(adapter, properties);
        var inboundPort    = portFactory.getInstanceOf(port, properties);
        Validate.notNull(inboundPort);
        drivingAdapter.register(inboundPort);

        compositeDrivingAdapter.add(drivingAdapter);
    }

    public void bindToPortWrapper(Class<? extends IDrivingAdapter> adapter, Class<?> portWrapper)
    {
        var drivingAdapter = drivingAdapterFactory.newInstanceOf(adapter, properties);

        var requiredPort = drivingAdapterFactory.requiredPort(portWrapper);

        if ( requiredPort != null ) {
            var inboundPort  = portFactory.getInstanceOf(requiredPort, properties);
            var newPortWrapper = ClassFactory.newInstanceOf(portWrapper, new Object[]{inboundPort});

            drivingAdapter.register(newPortWrapper.orElseThrow());
            compositeDrivingAdapter.add(drivingAdapter);
        }
    }


    public void bind(Class<? extends IDrivingAdapter> adapter, Object port) {
        Validate.notNull(adapter);
        Validate.notNull(port);

        var drivingAdapter = drivingAdapterFactory.getInstanceOf(adapter, properties);
        drivingAdapter.register(port);

        compositeDrivingAdapter.add(drivingAdapter);
    }
   
    public void bindToAnnotatedPorts(Class<? extends IDrivingAdapter> adapter, Class<? extends Annotation> portAnnotation) {
        Validate.notNull(adapter);
        Validate.notNull(portAnnotation);

        //Create ports and adapter
        var drivingAdapter = drivingAdapterFactory.getInstanceOf(adapter, properties);

        var portList = portFactory.getPortsBy(portAnnotation, properties);
        portList.forEach(drivingAdapter::register);
        
        compositeDrivingAdapter.add(drivingAdapter);
    }

    public <T> T newInstanceOfPort(Class<T> port)
    {
        return port.cast(portFactory.newInstanceOf(port, properties));
    }

    public <T> T getInstanceOfPort(Class<T> port)
    {
        return port.cast(portFactory.getInstanceOf(port, properties));
    }


    public void startDrivingAdapters()
    {
        compositeDrivingAdapter.start();
    }

    public void stopDrivingAdapters()
    {
        compositeDrivingAdapter.stop();
    }

    public BoundedContext getBoundedContext()
    {
        return boundedContext;
    }

    public void run()
    {
        setupSignalHandler();

        startDrivingAdapters();

        boundedContext.run();

        stopDrivingAdapters();
    }

    private void setupSignalHandler() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            JexxaLogger.getLogger(JexxaMain.class).info("Shutdown signal received ...");
            boundedContext.shutdown();
        }));
    }

}
