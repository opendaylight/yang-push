/*
 * Copyright © 2015 Copyright (c) 2015 cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.yangpush.impl.rev141210;

import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;

import static org.mockito.Mockito.mock;

public class YangpushModuleTest {
    @Test
    public void testCustomValidation() {
        YangpushModule module = new YangpushModule(mock(ModuleIdentifier.class), mock(DependencyResolver.class));

        // ensure no exceptions on validation
        // currently this method is empty
        module.customValidation();
    }

    @Test
    public void testCreateInstance() throws Exception {
        // configure mocks
/*        DependencyResolver dependencyResolver = mock(DependencyResolver.class);
        BindingAwareBroker broker = mock(BindingAwareBroker.class);
        DomBroker dombroker= mock(DomBroker.class);
        when(dependencyResolver.resolveInstance(eq(BindingAwareBroker.class), any(ObjectName.class), any(JmxAttribute.class))).thenReturn(broker);

        // create instance of module with injected mocks
        YangpushModule module = new YangpushModule(mock(ModuleIdentifier.class), dependencyResolver);

        // getInstance calls resolveInstance to get the broker dependency and then calls createInstance
        AutoCloseable closeable = module.getInstance();

        // verify that the module registered the returned provider with the broker
        verify(broker).registerProvider((YangpushProvider)closeable);

        // ensure no exceptions on close
        closeable.close();*/
    }
}
