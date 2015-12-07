/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yangpush.subscription;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.rev150105.PushUpdates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.rev150105.push.updates.PushUpdate;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

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

	/**
	 * Creating protected constructor for creating singleton instance
	 */
	protected YangpushSubscriptionEngine() {
		super();
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

}
