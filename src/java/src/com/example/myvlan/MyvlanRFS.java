package com.example.myvlan;

import java.util.Properties;

import com.tailf.conf.*;
import com.tailf.dp.*;
import com.tailf.dp.annotations.ServiceCallback;
import com.tailf.dp.proto.*;
import com.tailf.dp.services.ServiceContext;
import com.tailf.navu.*;
import com.tailf.ncs.template.*;

public class MyvlanRFS {

    private static final String VLAN_TEMPLATE = "myvlan-template";
    private static final String VLAN_ID = "VLAN-ID";
    private static final String DEVICE = "DEVICE";
    private static final String INTF_NAME = "INTF_NAME";
    private static final String INTF_ID = "INTF_ID";

    @ServiceCallback(servicePoint = "myvlan-servicepoint", callType = ServiceCBType.CREATE)
    public Properties create(ServiceContext context, NavuNode service, NavuNode ncsRoot, Properties opaque)
            throws ConfException {
        String servicePath = null;
        try {
            servicePath = service.getKeyPath();
            Template vlanTemplate = getTemplate(context);
            TemplateVariables vlanVar = new TemplateVariables();
            setupInterfaces(service,vlanTemplate,vlanVar);
        } catch (Exception e) {
            throw new DpCallbackException("Cannot create service " + servicePath, e);
        }
        return opaque;
    }

    Template getTemplate(ServiceContext context) throws ConfException {
        return new Template(context, VLAN_TEMPLATE);
    }

    private void setupInterfaces(NavuNode service,Template vlanTemplate,TemplateVariables vlanVar) throws Exception {
        NavuList interfaces = service.list("device-if");
        for (NavuContainer interfaceContainer : interfaces.elements()) {
            applyTemplate(service,vlanTemplate,vlanVar,interfaceContainer);
        }
    }

    private void applyTemplate(NavuNode service, Template vlanTemplate, TemplateVariables vlanVar,NavuContainer interfaceContainer) throws Exception {
        String deviceName = interfaceContainer.leaf("device").valueAsString();
        String interfaceType = interfaceContainer.getSelectedCase("interface").getTag();
        String interfaceName = interfaceContainer.leaf(interfaceType).valueAsString();
        String vlanId = service.leaf("vlan-id").valueAsString();
        if(MyvlanRFS.isValidVlan(vlanId)) {
            vlanVar.putQuoted(VLAN_ID, vlanId);
            vlanVar.putQuoted(DEVICE, deviceName);
            vlanVar.putQuoted(INTF_NAME, interfaceName);
            vlanVar.putQuoted(INTF_ID, interfaceType);
            vlanTemplate.apply(service, vlanVar);
        }
        else {
            throw new Exception();
        }
    }

    private static boolean isValidVlan(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            long value = Long.parseLong(str);
            return value >= 1 && value <= 4094;
        } catch (Exception ex) {
            return false;
        }
    }
}
