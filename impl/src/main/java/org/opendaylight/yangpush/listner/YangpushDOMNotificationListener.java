/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yangpush.listner;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.IetfDatastorePushListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.PushChangeUpdate;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.PushUpdate;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.SubscriptionModified;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.SubscriptionResumed;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.SubscriptionStarted;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.SubscriptionSuspended;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.SubscriptionTerminated;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.datastore.push.rev151015.push.update.Encoding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.rev150105.PushUpdates;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class YangpushDOMNotificationListener implements IetfDatastorePushListener, DOMNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(YangpushDOMNotificationListener.class);
    private DOMDataBroker globalDomDataBroker;
    private String subscription_id = "";

    static final QName PU_subId = QName.cachedReference(QName.create(PushUpdate.QNAME, "subscription-id"));
    static final QName PU_DataStoreContentsXML = QName
            .cachedReference(QName.create(PushUpdate.QNAME, "datastore-contents-xml"));
    static final QName PU_encoding = QName.cachedReference(QName.create(PushUpdate.QNAME, "encoding"));
    static final QName PU_timeofupdate = QName.cachedReference(QName.create(PushUpdate.QNAME, "time-of-update"));

    // Nodes to push notification data to mdsal datastore
    public YangInstanceIdentifier push_update_iid = null;
    //

    public YangpushDOMNotificationListener(DOMDataBroker globalDomDataBroker, String subscription_id) {
        this.globalDomDataBroker = globalDomDataBroker;
        this.subscription_id = subscription_id;
        this.push_update_iid = buildIID(subscription_id);
    }

    private YangInstanceIdentifier buildIID(String sub_id) {
        QName pushupdate = QName.create("urn:opendaylight:params:xml:ns:yang:yangpush", "2015-01-05", "push-update");
        org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder builder = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
                .builder();
        builder.node(PushUpdates.QNAME);//.node(pushupdate).nodeWithKey(pushupdate,
                //QName.create(pushupdate, "subscription-id"), sub_id);

        return builder.build();
    }

    @Override
    public void onNotification(DOMNotification notification) {
        // TODO Auto-generated method stub
        LOG.info("Notification recieved {}", notification.getBody());

        QName qname = PushUpdate.QNAME;
        SchemaPath schemaPath = SchemaPath.create(true, qname);

        if (notification.getType().equals(schemaPath)) {
            LOG.info("onPushUpdate schema..");
            try {
                DOMSource domSource = notificationToDomSource(notification);

            } catch (Exception e) {
                LOG.warn(e.toString());
            }
        }
    }

    private DOMSource notificationToDomSource(DOMNotification notification) {
        NodeIdentifier encoding = new NodeIdentifier(PU_encoding);
        NodeIdentifier contents = new NodeIdentifier(PU_DataStoreContentsXML);
        NodeIdentifier subid = new NodeIdentifier(PU_subId);
        NodeIdentifier timeofevent = new NodeIdentifier(PU_timeofupdate);
        ContainerNode conNode = notification.getBody();
        // DataContainerChild<? extends PathArgument, ?> valueNode = null;
        ChoiceNode valueNode = null;
        AnyXmlNode anyXmlValue = null;
        DOMSource domSource = null;
        String sub_id = "";
        String timeofeventupdate = "";
        try {
            sub_id = conNode.getChild(subid).get().getValue().toString();
            timeofeventupdate = conNode.getChild(timeofevent).get().getValue().toString();
            valueNode = (ChoiceNode) conNode.getChild(encoding).get();
            anyXmlValue = (AnyXmlNode) valueNode.getChild(contents).get();
            domSource = anyXmlValue.getValue();
        } catch (Exception e) {
            LOG.warn(e.toString());
        }
        String notificationAsString = domSourceToString(domSource);
        LOG.info("Notification recieved for sub_id :{} at : {}:\n {}", sub_id, timeofeventupdate, notificationAsString);

        storeToMdSal(sub_id, timeofeventupdate, domSource, notificationAsString);

        return domSource;
    }

    private void storeToMdSal(String sub_id, String timeofeventupdate, DOMSource domSource, String data) {
        NodeIdentifier subscriptionid = NodeIdentifier.create(QName.create(PushUpdates.QNAME, "subscription-id"));
        NodeIdentifier timeofupdate = NodeIdentifier.create(QName.create(PushUpdates.QNAME, "time-of-update"));
        NodeIdentifier datanode = NodeIdentifier.create(QName.create(PushUpdates.QNAME, "data"));
        NodeIdentifier encoding = NodeIdentifier.create(QName.create(PushUpdates.QNAME, "encoding"));
        NodeIdentifier datastorecontentsxml = NodeIdentifier
                .create(QName.create(PushUpdates.QNAME, "datastore-contents-xml"));
        NodeIdentifier pushupdates = NodeIdentifier.create(PushUpdates.QNAME);
        NodeIdentifier pushupdate = NodeIdentifier.create(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.rev150105.push.updates.PushUpdate.QNAME);
        
        DOMDataWriteTransaction tx = this.globalDomDataBroker.newWriteOnlyTransaction();
        YangInstanceIdentifier pid = YangInstanceIdentifier.builder()
                .node(PushUpdates.QNAME)
                .node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.rev150105.push.updates.PushUpdate.QNAME).build();
        
        final NormalizedNodeAttrBuilder<NodeIdentifier, DOMSource, AnyXmlNode> anyXmlBuilder = Builders.anyXmlBuilder()
                .withNodeIdentifier(datastorecontentsxml).withValue(domSource);
        AnyXmlNode val = anyXmlBuilder.build();
        
        ChoiceNode c = Builders.choiceBuilder().withNodeIdentifier(encoding)
                .withChild(val).build();
                //.withChild(ImmutableNodes.leafNode(datastorecontentsxml, notificationAsString)).build();
        
        NodeIdentifierWithPredicates p = new NodeIdentifierWithPredicates(
                QName.create(PushUpdates.QNAME, "push-update"),
                QName.create(PushUpdates.QNAME, "subscription-id"),
                sub_id);
        
        MapEntryNode men = ImmutableNodes.mapEntryBuilder().withNodeIdentifier(p)
                .withChild(ImmutableNodes.leafNode(subscriptionid, sub_id))
                .withChild(ImmutableNodes.leafNode(timeofupdate, timeofeventupdate))
                .withChild(ImmutableNodes.leafNode(datanode,data))
                //.withChild(ImmutableNodes.choiceNode(encoding.getNodeType()))
                //.withChild(val)
                //.withChild(c)
                .build();
        //Create Top Container node
        YangInstanceIdentifier iid = YangInstanceIdentifier.builder()
                .node(PushUpdates.QNAME).build();
        ContainerNode cn = Builders.containerBuilder().withNodeIdentifier(pushupdates).build();
        tx.merge(LogicalDatastoreType.CONFIGURATION, iid, cn);
        try {
            LOG.info("Store to container");
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        //
        YangInstanceIdentifier iid_1 = iid.node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.rev150105.push.updates.PushUpdate.QNAME);
        MapNode mn = Builders.mapBuilder().withNodeIdentifier(pushupdate).build();
        DOMDataWriteTransaction tx_1 = this.globalDomDataBroker.newWriteOnlyTransaction();
        tx_1.merge(LogicalDatastoreType.CONFIGURATION, iid_1, mn);
        try {
            LOG.info("Store to list");
            tx_1.submit().checkedGet();
        } catch (TransactionCommitFailedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        //create list node:
        
        DOMDataWriteTransaction tx_2 = this.globalDomDataBroker.newWriteOnlyTransaction();
        YangInstanceIdentifier yid = pid.node(new NodeIdentifierWithPredicates(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.rev150105.push.updates.PushUpdate.QNAME, men.getIdentifier().getKeyValues()));
        tx_2.merge(LogicalDatastoreType.CONFIGURATION, yid, men);
        LOG.info("--DATA PATh: {}\n--DATA\n{}",yid,men);
        try {
            LOG.info("Stores the list data");
            tx_2.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //
/*        YangInstanceIdentifier yid_1 = yid.node(encoding.getNodeType());
        DOMDataWriteTransaction tx_3 = this.globalDomDataBroker.newWriteOnlyTransaction();
        ChoiceNode chn = Builders.choiceBuilder().withNodeIdentifier(encoding).build();
        tx_3.merge(LogicalDatastoreType.CONFIGURATION, yid_1, chn);
        try {
            LOG.info("Create list/choice");
            tx_3.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
        //
/*        
        DOMDataWriteTransaction tx_4 = this.globalDomDataBroker.newWriteOnlyTransaction();
        tx_4.merge(LogicalDatastoreType.CONFIGURATION, yid_1, c);
        try {
            LOG.info("Create list/choice/data");
            tx_4.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
        
    }

    @Override
    public void onSubscriptionModified(SubscriptionModified notification) {
        // TODO Auto-generated method stub
        LOG.info("Notification recieved {}", notification);
    }

    @Override
    public void onSubscriptionResumed(SubscriptionResumed notification) {
        // TODO Auto-generated method stub
        LOG.info("Notification recieved {}", notification);
    }

    @Override
    public void onPushUpdate(PushUpdate notification) {
        // TODO Auto-generated method stub
        String subId = notification.getSubscriptionId().toString();
        String timeOfUpdate = notification.getTimeOfUpdate().toString();
        // notification.getEncoding();
        Class<? extends DataContainer> content = notification.getEncoding().getImplementedInterface();

        try {
            // content =
            // notification.getEncoding().getDatastoreContentsXml().toString();
        } catch (Exception e) {
            LOG.error("Only XML content supported!");
        }
        LOG.info("SubscriptionId: {}\nTimeOfUpdate: {}\ncontent: {}", subId, timeOfUpdate, content);
    }

    @Override
    public void onPushChangeUpdate(PushChangeUpdate notification) {
        // TODO Auto-generated method stub
        LOG.info("Notification recieved {}", notification);
    }

    @Override
    public void onSubscriptionSuspended(SubscriptionSuspended notification) {
        // TODO Auto-generated method stub
        LOG.info("Notification recieved {}", notification);
    }

    @Override
    public void onSubscriptionTerminated(SubscriptionTerminated notification) {
        // TODO Auto-generated method stub
        LOG.info("Notification recieved {}", notification);
    }

    @Override
    public void onSubscriptionStarted(SubscriptionStarted notification) {
        // TODO Auto-generated method stub
        LOG.info("Notification recieved {}", notification);
    }

    private String domSourceToString(DOMSource source) {
        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.transform(source, result);
            return writer.toString();
        } catch (Exception e) {
            LOG.warn(e.toString());
        }
        return null;
    }
}
