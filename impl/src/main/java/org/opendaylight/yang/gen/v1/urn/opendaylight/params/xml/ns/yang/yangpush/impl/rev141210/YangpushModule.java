/*
 * Copyright © 2015 Copyright (c) 2015 cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.impl.rev141210;

import org.opendaylight.yangpush.impl.YangpushDomProvider;

public class YangpushModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.impl.rev141210.AbstractYangpushModule {
    public YangpushModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public YangpushModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.impl.rev141210.YangpushModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        //YangpushProvider provider = new YangpushProvider();
        //getBrokerDependency().registerProvider(provider);
        
        final YangpushDomProvider provider = new YangpushDomProvider();
        getDomBrokerDependency().registerProvider(provider);
        
        return provider;
    }

}
