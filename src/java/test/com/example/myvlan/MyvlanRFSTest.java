package com.example.myvlan;

import com.tailf.conf.ConfException;
import com.tailf.dp.DpCallbackException;
import com.tailf.dp.services.ServiceContext;
import com.tailf.maapi.MaapiSchemas.CSCase;
import com.tailf.navu.*;
import com.tailf.ncs.template.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MyvlanRFSTest {

    @Spy
    private ServiceContext context;
    @Mock
    private NavuNode service;
    @Spy
    private NavuNode ncsRoot;
    @Spy
    private Properties opaque;
    @Spy
    private MyvlanRFS myvlanRFS;
    @Mock
    private Template vlanTemplate;
    @Mock
    private NavuLeaf leafService;
    @Mock
    private NavuList interfaces;
    List<NavuContainer> interfaceContainers;

    @BeforeEach
    void setup(){
        interfaceContainers = new ArrayList<>();
    }

    @ParameterizedTest(name = "{0} {1} {2} {3}")
    @CsvSource(value = { "1, FastEthernet, c0, 0/0", "10 , GigEthernet, c2, 0/1","1000, FastEthernet , c3, 0/0/0/1","4094, GigEthernet, c4, 1/1" })
    void givenCorrectVlanDataForAVlanAndOneInterface_whenExecuteCreate_thenGenerateService(String vlan, String interfaceType,String deviceName, String interfaceId) throws ConfException {
        // Given
        mockVlan(vlan);
        addInterface(interfaceType,interfaceId,deviceName);
        mockInterfaces();
        doReturn(vlanTemplate).when(myvlanRFS).getTemplate(context);
        // When
        Executable executable = () -> myvlanRFS.create(context, service, ncsRoot, opaque);
        // Then
        assertDoesNotThrow(executable);
    }

    @Test
    void givenCorrectVlanDataForAVlanAndOneInterface_whenExecuteCreate_thenTemplateApplyCalledOnlyOneTime() throws ConfException {
        // Given
        mockVlan("10");
        addInterface("FastEthernet","0/0","ios0");
        mockInterfaces();
        doReturn(vlanTemplate).when(myvlanRFS).getTemplate(context);
        // When
        myvlanRFS.create(context, service, ncsRoot, opaque);
        // Then
        verify(vlanTemplate,times(1)).apply(eq(service), any());
    }

    @Test
    void givenCorrectVlanDataForAVlanAndMultipleInterfaces_whenExecuteCreate_thenGenerateService() throws ConfException {
        // Given
        mockVlan("10");
        addInterface("FastEthernet","0/0","ios0");
        addInterface("FastEthernet","0/1","ios0");
        mockInterfaces();
        doReturn(vlanTemplate).when(myvlanRFS).getTemplate(context);
        // When
        Executable executable = () -> myvlanRFS.create(context, service, ncsRoot, opaque);
        // Then
        assertDoesNotThrow(executable);
    }

    @Test
    void givenCorrectVlanDataForAVlanAndMultipleInterfaces_whenExecuteCreate_thenCallTemplateApply2Times() throws ConfException {
        // Given
        mockVlan("10");
        addInterface("FastEthernet","0/0","ios0");
        addInterface("FastEthernet","0/1","ios0");
        mockInterfaces();
        doReturn(vlanTemplate).when(myvlanRFS).getTemplate(context);
        // When
        myvlanRFS.create(context, service, ncsRoot, opaque);
        // Then
        verify(vlanTemplate,times(2)).apply(eq(service), any());
    }

    @Test
    void givenCorrectVlanDataForAVlanAndThousandsInterfaces_whenExecuteCreate_thenGenerateServiceInLessThan500Ms() throws ConfException {
        // Given
        mockVlan("10");
        for(int i = 0; i < 1000; i++){
            addInterface("FastEthernet","0/"+i,"ios0");
        }
        mockInterfaces();
        doReturn(vlanTemplate).when(myvlanRFS).getTemplate(context);
        // When
        Executable executable = () -> myvlanRFS.create(context, service, ncsRoot, opaque);
        // Then
        assertTimeout(Duration.ofMillis(500), executable);
    }

    @Test
    void givenCorrectVlanDataForAVlanAndThousandsInterfaces_whenExecuteCreate_thenCalledOnly1000Time() throws ConfException {
        // Given
        mockVlan("10");
        for(int i = 0; i < 1000; i++){
            addInterface("FastEthernet","0/"+i,"ios0");
        }
        mockInterfaces();
        doReturn(vlanTemplate).when(myvlanRFS).getTemplate(context);
        // When
        myvlanRFS.create(context, service, ncsRoot, opaque);
        // Then
        verify(vlanTemplate,times(1000)).apply(eq(service), any());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(value = { "-1", "0","-1000","4095" })
    void givenWrongVlan_whenExecuteCreate_thenThrowException(String vlanId) throws ConfException {
        // Given
        mockVlan(vlanId);
        addInterface("FastEthernet","0/0","ios0");
        mockInterfaces();
        doReturn(vlanTemplate).when(myvlanRFS).getTemplate(context);
        // When
        Executable executable = () -> myvlanRFS.create(context, service, ncsRoot, opaque);
        // Then
        assertThrows(DpCallbackException.class,executable);
    }

    private void mockVlan(String vlanId) throws NavuException {
        when(service.getKeyPath()).thenReturn("vlan");
        when(leafService.valueAsString()).thenReturn(vlanId);
        when(service.leaf("vlan-id")).thenReturn(leafService);
    }

    private void mockInterfaces() throws NavuException {
        when(interfaces.elements()).thenReturn(interfaceContainers);
        when(service.list("device-if")).thenReturn(interfaces);
    }

    private void addInterface(String InterfaceType,String interfaceId,String deviceName) throws NavuException {
        NavuContainer interfaceContainer = mock(NavuContainer.class);
        CSCase interfaceCase = mock(CSCase.class);
        when(interfaceCase.getTag()).thenReturn(InterfaceType);
        when(interfaceContainer.getSelectedCase("interface")).thenReturn(interfaceCase);
        NavuLeaf leafDevice = mock(NavuLeaf.class);
        when(leafDevice.valueAsString()).thenReturn(deviceName);
        when(interfaceContainer.leaf("device")).thenReturn(leafDevice);
        NavuLeaf leafInterface = mock(NavuLeaf.class);
        when(leafInterface.valueAsString()).thenReturn(interfaceId);
        when(interfaceContainer.leaf(InterfaceType)).thenReturn(leafInterface);
        interfaceContainers.add(interfaceContainer);
    }
}
