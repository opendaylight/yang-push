/*
 * Copyright © 2015 Copyright (c) 2015 cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangpush.rpc;

import java.util.HashSet;
import java.util.Set;
import java.util.Collection;

import javax.xml.transform.dom.DOMSource;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.CreateSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.DeleteSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.IetfDatastorePushListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.IetfDatastorePushService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.ModifySubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.PushUpdate;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.subscription.info.UpdateTrigger;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.subscription.info.update.trigger.Periodic;
import org.opendaylight.yangpush.impl.YangpushDomProvider;
import org.opendaylight.yangpush.listner.YangpushDOMNotificationListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

public class YangpushRpcImpl implements DOMRpcImplementation {

    private static final Logger LOG = LoggerFactory.getLogger(YangpushRpcImpl.class);

    static final QName Y_PERIOD_NAME = QName.create("urn:opendaylight:params:xml:ns:yang:yangpush", "2015-01-05",
            "period");
    static final QName Y_SUB_ID_NAME = QName.create("urn:opendaylight:params:xml:ns:yang:yangpush", "2015-01-05",
            "subscription-id");
    static final QName Y_NODE_NAME = QName.create("urn:opendaylight:params:xml:ns:yang:yangpush", "2015-01-05",
            "node-name");
    static final QName I_PUSH_UPDATE_TRIGGER = QName.cachedReference(QName.create(UpdateTrigger.QNAME, "update-trigger"));
    static final QName I_PUSH_PERIOD_NAME = QName.cachedReference(QName.create(Periodic.QNAME, "period"));
    static final QName I_PUSH_TARGET_DATASTORE = QName
            .cachedReference(QName.create(CreateSubscriptionInput.QNAME, "target-datastore"));
    static final QName I_PUSH_STREAM = QName.cachedReference(QName.create(CreateSubscriptionInput.QNAME, "stream"));
    static final QName I_PUSH_ENCODING = QName.cachedReference(QName.create(CreateSubscriptionInput.QNAME, "encoding"));
    static final QName I_PUSH_START_TIME = QName
            .cachedReference(QName.create(CreateSubscriptionInput.QNAME, "start-time"));
    static final QName I_PUSH_STOP_TIME = QName
            .cachedReference(QName.create(CreateSubscriptionInput.QNAME, "stop-time"));
    static final QName I_PUSH_SUBTREE_FILTERSPEC = QName
            .cachedReference(QName.create(CreateSubscriptionInput.QNAME, "filterspec"));
    static final QName I_PUSH_SUBTREE_FILTER_TYPE = QName
            .cachedReference(QName.create(CreateSubscriptionInput.QNAME, "filter-type"));
    static final QName I_PUSH_SUBTREE_FILTER = QName
            .cachedReference(QName.create(CreateSubscriptionInput.QNAME, "subtree-filter"));
    static final QName I_PUSH_FILTER = QName
            .cachedReference(QName.create(CreateSubscriptionInput.QNAME, "filter"));

    //ietf cs nodes
    static final NodeIdentifier I_RPC_CS_INPUT = NodeIdentifier.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput.QNAME);
    static final QName I_RPC_STREAM_NAME = QName.cachedReference(QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput.QNAME,"stream"));
    static final QName I_RPC_STARTTIME_NAME = QName.cachedReference(QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput.QNAME,"startTime"));
    static final QName I_RPC_STOPTIME_NAME = QName.cachedReference(QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput.QNAME,"stopTime"));
    static final QName I_RPC_FILTER_NAME = QName.cachedReference(QName.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput.QNAME,"filter"));

    static final NodeIdentifier CS_STERAM_ARG = NodeIdentifier.create(I_RPC_STREAM_NAME);
    static final NodeIdentifier CS_STARTTIME_ARG = NodeIdentifier.create(I_RPC_STARTTIME_NAME);
    static final NodeIdentifier CS_STOPTIME_ARG = NodeIdentifier.create(I_RPC_STOPTIME_NAME);
    static final NodeIdentifier CS_PERIOD_ARG = NodeIdentifier.create(Y_PERIOD_NAME);
    static final NodeIdentifier CS_SUB_ID_ARG = NodeIdentifier.create(Y_SUB_ID_NAME);
    static final NodeIdentifier CS_FILTER_ARG = NodeIdentifier.create(I_RPC_FILTER_NAME);

    private static final DOMRpcIdentifier CREATE_SUBSCRIPTION_RPC = DOMRpcIdentifier
            .create(SchemaPath.create(true, QName.create(CreateSubscriptionInput.QNAME, "create-subscription")));
    private static final DOMRpcIdentifier MODIFY_SUBSCRIPTION_RPC = DOMRpcIdentifier
            .create(SchemaPath.create(true, QName.create(ModifySubscriptionInput.QNAME, "modify-subscription")));
    private static final DOMRpcIdentifier DELETE_SUBSCRIPTION_RPC = DOMRpcIdentifier
            .create(SchemaPath.create(true, QName.create(DeleteSubscriptionInput.QNAME, "delete-subscription")));

    //
    static final String NOTIFICATION_NS = "urn:ietf:params:xml:ns:netconf:notification:1.0";
    //
    private DOMRpcProviderService service;
    private DOMMountPointService mountPointService;

    private DOMDataBroker globalDomDataBroker;

    static private int sub_id = 0;

    // Error messages
    public static String ERR_INVALID_INPUT = "Invalid input";

    public YangpushRpcImpl(DOMRpcProviderService service, DOMMountPointService mountPointService, DOMDataBroker globalDomDataBroker) {
        super();
        this.service = service;
        this.mountPointService = mountPointService;
        this.globalDomDataBroker = globalDomDataBroker;
        registerRPCs();
    }

    private void registerRPCs() {
        service.registerRpcImplementation(this, CREATE_SUBSCRIPTION_RPC, MODIFY_SUBSCRIPTION_RPC,
                DELETE_SUBSCRIPTION_RPC);
    }

    /**
     * This method is invoked on RPC invocation of the registered method.
     * rpc(localname) is used to invoke the correct requested method.
     */
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(DOMRpcIdentifier rpc, NormalizedNode<?, ?> input) {
        if (rpc.equals(CREATE_SUBSCRIPTION_RPC)) {
            LOG.debug("This is a create subscription RPC");
            sub_id++;
            return createSubscriptionRpcHandler(input);
        } else if (rpc.equals(MODIFY_SUBSCRIPTION_RPC)) {
            LOG.info("This is a modify subscrition RPC. Not supported ...");
        } else if (rpc.equals(DELETE_SUBSCRIPTION_RPC)) {
            LOG.info("This is a delete subscrition RPC. Not supported ...");
        } else {
            LOG.info("Unknown RPC...");
        }

        return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult());
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> createSubscriptionRpcHandler(NormalizedNode<?, ?> input) {
        String error = "";
        CreateSubscriptionRpcInput inputData = parseExternalRpcInput(input, error);
        String sid = Integer.toString(sub_id);
        inputData.setSubscription_id(sid);
        if (inputData == null) {
            // handle error
            LOG.error(error);
            return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult());
        }
        final Optional<DOMMountPoint> mountPoint = getMountPoint(inputData.getNode_name(), error);
        if (!mountPoint.isPresent()) {
            LOG.error(error);
            return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult());
        }

        // register notification listener
        YangpushDOMNotificationListener listener = new YangpushDOMNotificationListener(this.globalDomDataBroker, inputData.getSubscription_id());
        final Optional<DOMNotificationService> service = mountPoint.get().getService(DOMNotificationService.class);
        //mountPoint.get().getService(.class);
        QName qname = PushUpdate.QNAME;
        SchemaPath schemaPath = SchemaPath.create(true, qname);
        final ListenerRegistration<YangpushDOMNotificationListener> accessTopologyListenerListenerRegistration = service
                .get().registerNotificationListener(listener, schemaPath);

        final Optional<DOMRpcService> rpcService = mountPoint.get().getService(DOMRpcService.class);
        QName uri = QName.create("urn:ietf:params:xml:ns:netconf:notification:1.0", "2008-07-14",
                "create-subscription");
        SchemaPath type = SchemaPath.create(true, uri);

        ContainerNode cn = createDeviceCSRpcInput(inputData, error);

        CheckedFuture<DOMRpcResult, DOMRpcException> result = rpcService.get().invokeRpc(type, cn);

        return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult());
    }

    private CreateSubscriptionRpcInput parseExternalRpcInput(NormalizedNode<?, ?> input, String error) {
        CreateSubscriptionRpcInput csri = new CreateSubscriptionRpcInput();
        ContainerNode conNode = null;
        error = "";
        if (input == null) {
            error = ERR_INVALID_INPUT;
            return null;
        }

        if (input instanceof ContainerNode) {
            conNode = (ContainerNode) input;
            try {
                //ImmutableAugmentationNode{nodeIdentifier=AugmentationIdentifier{childNames=[(urn:opendaylight:params:xml:ns:yang:yangpush?revision=2015-01-05)node-name]}, value=[ImmutableLeafNode{nodeIdentifier=(urn:opendaylight:params:xml:ns:yang:yangpush?revision=2015-01-05)node-name, value=test1, attributes={}}]}
                DataContainerChild<? extends PathArgument, ?> nodeName = null;
                //NodeIdentifier nodeId = new NodeIdentifier(Y_NODE_NAME);
                // create extension nodes for yang-push
                Set<QName> childNames = new HashSet<>();
                childNames.add(Y_NODE_NAME);
                AugmentationIdentifier ai = new AugmentationIdentifier(childNames);
                Set<LeafNode<?>> nodeWithName = (Set<LeafNode<?>>) conNode.getChild(ai).get().getValue();
                nodeName = (LeafNode<?>) nodeWithName.toArray()[0];
                if (nodeName.getValue() != null) {
                    csri.setNode_name(nodeName.getValue().toString());
                } else {
                    error = "Invalid input argument node name. Value is NULL";
                    return null;
                }

                //ImmutableChoiceNode{nodeIdentifier=(urn:ietf:params:xml:ns:yang:ietf-datastore-push?revision=2015-10-15)update-trigger, value=[ImmutableLeafNode{nodeIdentifier=(urn:ietf:params:xml:ns:yang:ietf-datastore-push?revision=2015-10-15)period, value=10, attributes={}}]}
                DataContainerChild<? extends PathArgument, ?> periodNode = null;
                //NodeIdentifier period = new NodeIdentifier(I_PUSH_PERIOD_NAME);
                NodeIdentifier updateTrigger = new NodeIdentifier(I_PUSH_UPDATE_TRIGGER);
                //TODO what about the other case (on-change) ?
                Set<LeafNode<?>> nodeWithPeriodic = (Set<LeafNode<?>>) conNode.getChild(updateTrigger).get().getValue();
                periodNode = (LeafNode<?>) nodeWithPeriodic.toArray()[0];
                if (periodNode.getValue() != null) {
                    Long periodl = new Long(periodNode.getValue().toString());
                    csri.setPeriod(periodl);
                } else {
                    error = "Invalid input period. Value is NULL";
                    return null;
                }

                DataContainerChild<? extends PathArgument, ?> streamNode = null;
                NodeIdentifier stream = new NodeIdentifier(I_PUSH_STREAM);
                streamNode = conNode.getChild(stream).get();
                if (streamNode.getValue() != null) {
                    QName streamName = (QName) streamNode.getValue();
                    String name = streamName.getLocalName();
                    csri.setStream_name(name);
                } else {
                    error = "Invalud input stream name. Value is NULL";
                    return null;
                }

                //ImmutableChoiceNode{nodeIdentifier=(urn:ietf:params:xml:ns:yang:ietf-datastore-push?revision=2015-10-15)filterspec, value=
                //[ImmutableChoiceNode{nodeIdentifier=(urn:ietf:params:xml:ns:yang:ietf-datastore-push?revision=2015-10-15)filter-type, value=
                //[ImmutableXmlNode{nodeIdentifier=(urn:ietf:params:xml:ns:yang:ietf-datastore-push?revision=2015-10-15)subtree-filter, value=
                //javax.xml.transform.dom.DOMSource@366a5a0a, attributes={}}]}]},
                NodeIdentifier filterspec = new NodeIdentifier(I_PUSH_SUBTREE_FILTERSPEC);
                NodeIdentifier filtertype = new NodeIdentifier(I_PUSH_SUBTREE_FILTER_TYPE);
                NodeIdentifier subtreeFilter = new NodeIdentifier(I_PUSH_SUBTREE_FILTER);
                DataContainerChild<? extends PathArgument, ?> t = conNode.getChild(filterspec).get();
                ChoiceNode t1 = (ChoiceNode) t;
                DataContainerChild<? extends PathArgument, ?> t2 = t1.getChild(filtertype).get();
                ChoiceNode t3 = (ChoiceNode) t2;
                DataContainerChild<? extends PathArgument, ?> t4 = t3.getChild(subtreeFilter).get();
                if (t4 != null) {
                    AnyXmlNode anyXmlFilter = (AnyXmlNode) t4;
                    org.w3c.dom.Node nodeFilter = anyXmlFilter.getValue().getNode();
                    org.w3c.dom.Document document = nodeFilter.getOwnerDocument();
                    document.renameNode(nodeFilter, NOTIFICATION_NS, "filter");
                    DOMSource domSource = anyXmlFilter.getValue();
                    csri.setFilter(domSource);
                } else {
                    error = "Invalid input filter. Value is NULL";
                }
            } catch (Exception e) {
                LOG.error(e.toString());
            }
        } else {
            return null;
        }
        return csri;
    }

    private Optional<DOMMountPoint> getMountPoint(String node_name, String error) {
        // to get the mountPoint of the Device
        Optional<DOMMountPoint> mountPoint = null;
        try {
            QName net_topo = QName.create("urn:TBD:params:xml:ns:yang:network-topology", "2013-10-21",
                    "network-topology");
            QName topo = QName.create(net_topo, "topology");
            QName topoId = QName.create(topo, "topology-id");
            QName fromNode = QName.create(net_topo, "node");
            QName fromIdName = QName.create(fromNode, "node-id");
            // The InstanceIdentifier of our device we want to invoke the
            // subscription on
            YangInstanceIdentifier iid = YangInstanceIdentifier.builder().node(net_topo).node(topo)
                    .nodeWithKey(topo, topoId, "topology-netconf").node(fromNode)
                    .nodeWithKey(fromNode, fromIdName, node_name).build();

            boolean present = YangpushDomProvider.NETCONF_TOPO_IID.contains(iid);
            // The mount of the device, or more accurate: an Optional, with our
            // MountPoint
            if (present) {
                mountPoint = mountPointService.getMountPoint(iid);
            } else {
                error = "Mount point is not available for node_name = " + node_name;
            }
        } catch (Exception e) {
            throw e;
        }
        return mountPoint;
    }

    private ContainerNode createDeviceCSRpcInput(CreateSubscriptionRpcInput inputData, String error) {
        // create input anyxml filter
        final NormalizedNodeAttrBuilder<NodeIdentifier, DOMSource, AnyXmlNode> anyXmlBuilder = Builders.anyXmlBuilder()
                .withNodeIdentifier(CS_FILTER_ARG).withValue(inputData.getFilter());
        AnyXmlNode val = anyXmlBuilder.build();

        // create extension nodes for yang-push
        Set<QName> childNames = new HashSet<>();
        childNames.add(Y_PERIOD_NAME);
        childNames.add(Y_SUB_ID_NAME);
        AugmentationIdentifier ai = new AugmentationIdentifier(childNames);

        AugmentationNode an = Builders.augmentationBuilder().withNodeIdentifier(ai)
                .withChild(ImmutableNodes.leafNode(Y_PERIOD_NAME, inputData.getPeriod()))
                .withChild(ImmutableNodes.leafNode(Y_SUB_ID_NAME, inputData.getSubscription_id())).build();

        final ContainerNode cn = Builders.containerBuilder().withNodeIdentifier(I_RPC_CS_INPUT)
                .withChild(ImmutableNodes.leafNode(CS_STERAM_ARG, inputData.getStream_name())).withChild(val)
                .withChild(an).build();

        return cn;
    }

    final class CreateSubscriptionRpcInput {

        public CreateSubscriptionRpcInput() {
            this.period = new Long("0");
            this.filter = null;
            this.node_name = "";
            this.stream_name = "";
            this.subscription_id = "";
        }

        public Long getPeriod() {
            return period;
        }

        public void setPeriod(Long period) {
            this.period = period;
        }

        public String getNode_name() {
            return node_name;
        }

        public void setNode_name(String node_name) {
            this.node_name = node_name;
        }

        public String getStream_name() {
            return stream_name;
        }

        public void setStream_name(String stream_name) {
            this.stream_name = stream_name;
        }

        public String getSubscription_id() {
            return subscription_id;
        }

        public void setSubscription_id(String subscription_id) {
            this.subscription_id = subscription_id;
        }

        public DOMSource getFilter() {
            return filter;
        }

        public void setFilter(DOMSource filter) {
            this.filter = filter;
        }

        private Long period;
        private String node_name;
        private String stream_name;
        private String subscription_id;
        private DOMSource filter;
    }

}
