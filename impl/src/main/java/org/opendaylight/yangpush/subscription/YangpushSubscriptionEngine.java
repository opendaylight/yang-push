/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yangpush.subscription;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.rev150105.PushUpdates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.rev150105.push.updates.PushUpdate;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * This is a singleton class for handing all subscription related data
 * for YANG-PUSH at MD-SAL.
 *
 * @author Ambika.Tripathy
 *
 */
public class YangpushSubscriptionEngine {
	// global data broker
	private DOMDataBroker globalDomDataBroker = null;
	// self instance
	private static YangpushSubscriptionEngine instance = null;
	//Subscription ID sid
	private static int sub_id = -1;
	// map of subscriptions //TODO: need optimization for scale
	//key is subscription-id and value use node name
	private Map<String,String> masterSubMap = null;

    /**
     * operations used to update subscription info to MD-SAL
     * @author ambtripa
     *
     */
    public static enum operations {
        create,
        update,
        delete,
    }

	/**
	 * Creating protected constructor for creating singleton instance
	 */
	protected YangpushSubscriptionEngine() {
	    super();
            masterSubMap = new HashMap<String, String>();
	}

	/**
	 * getInstance method implements subscription engine as singleton
	 * @return this
	 */
	public static YangpushSubscriptionEngine getInstance() {
		if(instance == null) {
			instance = new YangpushSubscriptionEngine();
		}
		return instance;
	}

	/**
	 *  Set global BI data broker to subscription engine
	 * @param globalDomDataBroker
	 */
	public void setDataBroker(DOMDataBroker globalDomDataBroker) {
		this.globalDomDataBroker = globalDomDataBroker;
	}


	/**
	* Creates data store for push-update container at startup.
	* This data store contains the subscription push-updates.
	*
	*  container push-updates {
        *   list push-update {
        *   ----
        *   ----
        *   }
        *  }
        *
	*/
	public void createPushUpdateDataStore(){
	    DOMDataWriteTransaction tx = this.globalDomDataBroker.newWriteOnlyTransaction();
            NodeIdentifier pushupdates = NodeIdentifier.create(PushUpdates.QNAME);
            NodeIdentifier pushupdate = NodeIdentifier.create(PushUpdate.QNAME);
            YangInstanceIdentifier iid = YangInstanceIdentifier.builder()
                .node(PushUpdates.QNAME).build();
            //Creates container node push-update in BI way and
            //commit to MD-SAL at the start of the application.
            ContainerNode cn = Builders.containerBuilder().withNodeIdentifier(pushupdates).build();
            tx.merge(LogicalDatastoreType.CONFIGURATION, iid, cn);
            try {
                tx.submit().checkedGet();
            } catch (TransactionCommitFailedException e1) {
                e1.printStackTrace();
            }
            //Creates push-update list node and BI way and
            //commit to MD-SAL at the start of the application.
            YangInstanceIdentifier iid_1 = iid.node(PushUpdate.QNAME);
            MapNode mn = Builders.mapBuilder().withNodeIdentifier(pushupdate).build();
            DOMDataWriteTransaction tx_1 = this.globalDomDataBroker.newWriteOnlyTransaction();
            tx_1.merge(LogicalDatastoreType.CONFIGURATION, iid_1, mn);
            try {
                tx_1.submit().checkedGet();
            } catch (TransactionCommitFailedException e1) {
                e1.printStackTrace();
            }
	}

	/**
	 * Generate a subscriptionId for a subscription. This method
	 * ensures all subscriptionId generated for the session accross all
	 * nodes must be unique.
	 *
	 * NOTE: Overflow of subscriptionId will happen if subscriptionId
	 * reach to max of int type.
	 * @return
	 */
	public String generateSubscriptionId(){
		this.sub_id++;
		return "20"+Integer.toString(this.sub_id);
	}

	/**
	 * Checks the subscriptionId is present in subscription database
	 * @param sub_id
	 * @param node_name
	 * @return
	 */
	public boolean isSubscriptionPresent(String sub_id, String node_name){
		return this.masterSubMap.containsKey(sub_id);
	}

        /**
         * Method used to populate md-sal datastore for yangpush model
         * Updates the subscriptionId and Node name for a subscription when there is a
         * creation, modification or delete to a subscription.
         *
         * @param sub_id
         * @param node_name
         * @param type
         */
        public void updateSubscriptiontoMdSal(String sub_id, String node_name, operations type) {
            NodeIdentifier subscriptionId = NodeIdentifier.create(QName.create(PushUpdates.QNAME, "subscription-id"));
            NodeIdentifier nodeName = NodeIdentifier.create(QName.create(PushUpdates.QNAME, "node-name"));
            YangInstanceIdentifier pid = YangInstanceIdentifier.builder()
                .node(PushUpdates.QNAME)
                .node(PushUpdate.QNAME).build();

            NodeIdentifierWithPredicates p = new NodeIdentifierWithPredicates(
                QName.create(PushUpdates.QNAME, "push-update"),
                QName.create(PushUpdates.QNAME, "subscription-id"),
                sub_id);

            MapEntryNode men = ImmutableNodes.mapEntryBuilder().withNodeIdentifier(p)
                .withChild(ImmutableNodes.leafNode(subscriptionId, sub_id))
                .withChild(ImmutableNodes.leafNode(nodeName,node_name))
                .build();

            DOMDataWriteTransaction tx = this.globalDomDataBroker.newWriteOnlyTransaction();
            YangInstanceIdentifier yid = pid.node(new NodeIdentifierWithPredicates(PushUpdate.QNAME, men.getIdentifier().getKeyValues()));

            switch (type){
                case create:
                    tx.merge(LogicalDatastoreType.CONFIGURATION, yid, men);
                    masterSubMap.put(sub_id, node_name);
                    break;
                case delete:
                    tx.delete(LogicalDatastoreType.CONFIGURATION, yid);
                    masterSubMap.remove(sub_id, node_name);
                    break;
                case update:
                    tx.merge(LogicalDatastoreType.CONFIGURATION, yid, men);
                    masterSubMap.put(sub_id, node_name);
                    break;
                default:
                    break;
            }
            try {
                tx.submit().checkedGet();
            } catch (TransactionCommitFailedException e) {
                e.printStackTrace();
            }
        }
}
