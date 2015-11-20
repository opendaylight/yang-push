/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yangpush.impl;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opendaylight.yangpush.impl.handlers.LoggingNotificationHandler;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.Provider.ProviderFunctionality;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.generic.notifications.rev150611.GenericNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.rev150105.CsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

public class SubscriptionService implements Provider, AutoCloseable, DOMRpcImplementation, DOMDataChangeListener{
	
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionService.class);
    public static final YangInstanceIdentifier NETCONF_TOPO_IID;
    
    private static final DOMRpcIdentifier CREATE_SUBSCRIPTION_ID = DOMRpcIdentifier.create(SchemaPath.create(true, QName.create(CsInput.QNAME, "cs")));
    
    //ietf cs nodes
    static final NodeIdentifier CS_INPUT = NodeIdentifier.create(CreateSubscriptionInput.QNAME);
    static final QName I_STREAM_NAME = QName.cachedReference(QName.create(CreateSubscriptionInput.QNAME,"stream"));
    static final QName I_FILTER_NAME = QName.cachedReference(QName.create(CreateSubscriptionInput.QNAME,"filter"));

    static final NodeIdentifier CS_STERAM_ARG = NodeIdentifier.create(I_STREAM_NAME);
    static final NodeIdentifier CS_FILTER_ARG = NodeIdentifier.create(I_FILTER_NAME);
    
    // cs nodes
    static final QName CS_NODE_NAME = QName.cachedReference(QName.create(CsInput.QNAME,"node-name"));
    static final QName CS_STREAM = QName.cachedReference(QName.create(CsInput.QNAME,"stream"));
    static final QName CS_PERIOD = QName.cachedReference(QName.create(CsInput.QNAME,"period"));
    static final QName CS_SUBSCRIPTION_ID = QName.cachedReference(QName.create(CsInput.QNAME,"subscription-id"));
    static final QName CS_FILTER = QName.cachedReference(QName.create(CsInput.QNAME,"filter"));
    
    //Augmented Nodes
    static final QName Y_PERIOD_NAME = QName.create("urn:opendaylight:params:xml:ns:yang:yangpush","2015-10-12","period");
    static final QName Y_SUB_ID_NAME = QName.create("urn:opendaylight:params:xml:ns:yang:yangpush", "2015-10-12","subscription-id");
   
   
    private DOMMountPointService mountPointService;
    private DOMDataBroker globalDomDataBroker;
    
    static {
        final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder builder =
                org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder();
        builder
        .node(NetworkTopology.QNAME)
        .node(Topology.QNAME)
        .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), TopologyNetconf.QNAME.getLocalName());

        NETCONF_TOPO_IID = builder.build();
    }

    /**
     * This method initializes DomDataBroker and Mountpoint service.
     * This services needed throughout the lifetime of the ncmount
     * application and registers its RPC implementation and Data change
     * Listener with the MD-SAL.
     */
    @Override
	public void onSessionInitiated(final Broker.ProviderSession providerSession) {
        // get the DOM versions of MD-SAL services
        this.globalDomDataBroker = providerSession.getService(DOMDataBroker.class);
        this.mountPointService = providerSession.getService(DOMMountPointService.class);
        
        SchemaContext schemaContext = null;

        final DOMRpcProviderService service = providerSession.getService(DOMRpcProviderService.class);
        service.registerRpcImplementation(this, CREATE_SUBSCRIPTION_ID);

        final YangInstanceIdentifier nodeIid = YangInstanceIdentifier.builder(NETCONF_TOPO_IID).node(Node.QNAME).build();

        LOG.info("SubscriptionService is registered");

        this.globalDomDataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                nodeIid,
                this,
                AsyncDataBroker.DataChangeScope.SUBTREE);
    }

    /**
     * From AutoCloaseable
     */
    @Override
    public void close() {
        this.globalDomDataBroker = null;
        this.mountPointService = null;
    }

    /**
     * From Provider
     */
    @Deprecated
    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        // Deprecated, not using
        return Collections.emptySet();
    }
    
    /**
     * This method is SubscriptionService's Data Change Listener on the Netconf Topology
     * namespace. It registers at the root of the Netconf Topology subtree for
     * changes in the entire subtree. At this point the method only logs the
     * data change to demonstrate the basic design pattern. A real application
     * would use the the data contained in the data change event to, for
     * example, maintain paths to connected netconf nodes.
     *
     * @param change Data change event
     */
    @Override
    public void onDataChanged(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        // TODO: Method need to be implemented.
        //LOG.info("data changed: {}", change);
    }
    
    /**
     * This method is invoked on RPC invocation of the registered
     * method. domRpcIdentifier(localname) is used to invoke the
     * correct requested method.
     */
    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final DOMRpcIdentifier domRpcIdentifier, final NormalizedNode<?, ?> normalizedNode) {

        if (domRpcIdentifier.equals(CREATE_SUBSCRIPTION_ID)) {
            LOG.debug("This is a create subscription RPC");
            cs(normalizedNode);
            return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult());
        }

        return null;
    }
    
    /**
     * Our Create Subscription
     * @param normalizedNode
     * @return
     */
    public CheckedFuture<DOMRpcResult, DOMRpcException> cs(final NormalizedNode<?, ?> normalizedNode) {
    	
    	registerNotificationHandlerForCsBi(normalizedNode);	
    	return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult());
    }  

    public void registerNotificationHandlerForCsBi(NormalizedNode<?,?> inputNode){
    	DataContainerChild<? extends PathArgument, ?> nodeName = null;
    	DataContainerChild<? extends PathArgument, ?> periodNode = null;
    	DataContainerChild<? extends PathArgument, ?> streamNode = null;
    	DataContainerChild<? extends PathArgument, ?> subscriptionIdNode = null;
    	AnyXmlNode  anyXmlFilter = null;
    	ContainerNode conNode = null;
    	String filterString = null;
    	DOMSource domSource = null;
    	DataContainerChild<? extends PathArgument, ?> filterNode = null;
    	Set<QName> childNames = new HashSet<>();
    	childNames.add(Y_PERIOD_NAME);
    	childNames.add(Y_SUB_ID_NAME);
    	AugmentationIdentifier ai = new AugmentationIdentifier(childNames);
    	
    	//Parse inputs
    	if(inputNode instanceof ContainerNode){
    		conNode = (ContainerNode) inputNode;
    		try {
        		NodeIdentifier nodeId = new NodeIdentifier(CS_NODE_NAME);
        		nodeName = conNode.getChild(nodeId).get();
        		
        		NodeIdentifier period = new NodeIdentifier(CS_PERIOD);
        		periodNode = conNode.getChild(period).get();
        		
        		NodeIdentifier stream = new NodeIdentifier(CS_STREAM);
        		streamNode = conNode.getChild(stream).get();
        		
        		NodeIdentifier subscriptionId = new NodeIdentifier(CS_SUBSCRIPTION_ID);
        		subscriptionIdNode = conNode.getChild(subscriptionId).get();
        		
        		NodeIdentifier filter = new NodeIdentifier(CS_FILTER);
        		filterNode = conNode.getChild(filter).get();
        	    anyXmlFilter = (AnyXmlNode) filterNode;

				String notificationNS = "urn:ietf:params:xml:ns:netconf:notification:1.0";

				org.w3c.dom.Node nodeFilter = anyXmlFilter.getValue().getNode();
				org.w3c.dom.Document document = nodeFilter.getOwnerDocument();
				document.renameNode(nodeFilter, notificationNS, nodeFilter.getNodeName());

        		
        		domSource = anyXmlFilter.getValue();
        	    StringWriter writer = new StringWriter();
        	    StreamResult result = new StreamResult(writer);
        	    TransformerFactory tf = TransformerFactory.newInstance();
        	    Transformer transformer = tf.newTransformer();
        	    transformer.transform(domSource, result);
        	    filterString = writer.toString();
        	   

        		
        		LOG.info("Register on {}, {} , {} , {}: {}",
        				nodeName.getValue().toString(),
        				periodNode.getValue().toString(),
        				streamNode.getValue().toString(),
        				subscriptionIdNode.getValue().toString(),
        				filterString
        				);
        		
    		} catch (Exception e){
    			LOG.error(e.toString());
    		}
    	}
    	
    	Long periodl = new Long(periodNode.getValue().toString());
    	
    	//to get the mountPoint of the Device
    	final Optional<DOMMountPoint> mountPoint;
    	try{
    		QName net_topo = QName.create("urn:TBD:params:xml:ns:yang:network-topology","2013-10-21", "network-topology");
    		QName topo = QName.create(net_topo, "topology");
    		QName topoId = QName.create(topo, "topology-id");
    		QName fromNode = QName.create(net_topo,"node");
        	QName fromIdName = QName.create(fromNode,"node-id");
        	//The InstanceIdentifier of our device we want to invoke the subscription on
    		YangInstanceIdentifier iid = YangInstanceIdentifier.builder()
    				.node(net_topo)
    				.node(topo)
    				.nodeWithKey(topo, topoId, "topology-netconf")
    				.node(fromNode)
    				.nodeWithKey(fromNode,fromIdName,nodeName.getValue().toString()).build();
        	
    		boolean bool = NETCONF_TOPO_IID.contains(iid);
    		//The mount of the device, or more accurate: an Optional, with our MountPoint
    	    mountPoint = mountPointService.getMountPoint(iid);
    		LOG.info("{}",mountPoint);
    	} catch (Exception e){
    		throw e;
    	}
    	
    	//Just like LoggingNotificationListner in the BA approach
        LoggingNotificationHandler listener = new LoggingNotificationHandler();
        final Optional<DOMNotificationService> service = mountPoint.get().getService(DOMNotificationService.class);
        LOG.info("YANG-PUSH Registering notification listener for node: {}", nodeName.getValue().toString());
        QName qname = GenericNotification.QNAME;
        SchemaPath schemaPath = SchemaPath.create(true, qname);
        final ListenerRegistration<LoggingNotificationHandler> accessTopologyListenerListenerRegistration = service.get().registerNotificationListener(listener, schemaPath);
 
        //encode create-subscription message
        final Optional<DOMRpcService> rpcService = mountPoint.get().getService(DOMRpcService.class);
    	QName uri = QName.create("urn:ietf:params:xml:ns:netconf:notification:1.0","2008-07-14","create-subscription");
    	SchemaPath type = SchemaPath.create(true, uri); //path to createSubscription;
    	SchemaPath path = SchemaPath.create(true, uri, CreateSubscriptionInput.QNAME);
        
    	final NormalizedNodeAttrBuilder<NodeIdentifier, DOMSource, AnyXmlNode> anyXmlBuilder =
                Builders.anyXmlBuilder().withNodeIdentifier(CS_FILTER_ARG).withValue(domSource);
    	
       	AnyXmlNode val = anyXmlBuilder.build();
       	
       	AugmentationNode t = Builders.augmentationBuilder().withNodeIdentifier(ai)
       	.withChild(ImmutableNodes.leafNode(Y_PERIOD_NAME, periodl))
       	.withChild(ImmutableNodes.leafNode(Y_SUB_ID_NAME, "100-test")).build();
       	   	       	
       	
       	final ContainerNode cn = Builders.containerBuilder().withNodeIdentifier(CS_INPUT)
     		   .withChild(ImmutableNodes.leafNode(CS_STERAM_ARG, "push-update"))
     		   .withChild(val)
     		   .withChild(t).build();
       	
       	//invoke the rpc on the device
   		CheckedFuture<DOMRpcResult, DOMRpcException> result= rpcService.get().invokeRpc(type, cn);
       	LOG.info("Subscription created");
    }    
    
}
