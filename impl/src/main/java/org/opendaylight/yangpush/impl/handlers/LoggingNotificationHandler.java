/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangpush.impl.handlers;

import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.generic.notifications.rev150611.GenericNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.generic.notifications.rev150611.GenericNotificationsListener;
import org.opendaylight.yangpush.impl.SubscriptionService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoggingNotificationHandler implements GenericNotificationsListener, DOMNotificationListener {

	private static final Logger LOG = LoggerFactory.getLogger(SubscriptionService.class);
    
	@Override
	public void onGenericNotification(final GenericNotification notification){
		LOG.info("Generic Notification recieved {}", notification);
	}
	
    static final QName GN_value = QName.cachedReference(QName.create(GenericNotification.QNAME,"value"));
    
	@Override
	public void onNotification(DOMNotification notification) {
		// TODO Auto-generated method stub
		try{
		NodeIdentifier genNotifcValue = new NodeIdentifier(GN_value);
		ContainerNode conNode = notification.getBody();
    	DataContainerChild<? extends PathArgument, ?> valueNode = null;
    	AnyXmlNode  anyXmlValue = null;
    	DOMSource domSource = null;
		try{
			valueNode = conNode.getChild(genNotifcValue).get();
			anyXmlValue = (AnyXmlNode) valueNode;
			domSource = anyXmlValue.getValue();
		}catch (Exception e){
			LOG.warn(e.toString());
		}
	    StringWriter writer = new StringWriter();
	    StreamResult result = new StreamResult(writer);
	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer = tf.newTransformer();
	    transformer.transform(domSource, result);
	    String filterString = writer.toString();
		LOG.info("Notification recieved \n{}", filterString);
		}catch (Exception e){
			LOG.warn(e.toString());
		}
	}
	
}
