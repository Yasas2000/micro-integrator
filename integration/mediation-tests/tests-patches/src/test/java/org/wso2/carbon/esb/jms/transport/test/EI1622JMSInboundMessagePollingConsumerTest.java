/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.esb.jms.transport.test;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.extensions.servers.jmsserver.client.JMSQueueMessageProducer;
import org.wso2.carbon.automation.extensions.servers.jmsserver.controller.config.JMSBrokerConfigurationProvider;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.Utils;

import static org.testng.Assert.assertTrue;

/**
 * This is the test class for the fix product-ei/issues/1622.
 */
public class EI1622JMSInboundMessagePollingConsumerTest extends ESBIntegrationTest {
    private static final String ENDPOINT_NAME = "jms_inbound";
    private CarbonLogReader carbonLogReader = null;

    @BeforeClass(alwaysRun = true)
    protected void init() throws Exception {
        carbonLogReader = new CarbonLogReader();
        carbonLogReader.start();
    }

    @Test(groups = { "wso2.esb" },
            description = "Check whether polling is suspended.")
    public void testPollingWithSuspensionLimit() throws Exception {

        pushMessageToQue(addEndpoint());

        assertTrue(Utils.checkForLog(carbonLogReader, "Suspending polling as the pollingSuspensionLimit of 2 "
                           + "reached. Polling will be re-started after 3000 milliseconds", 180),
                   "JMS Polling suspension is not enabled.");
        Utils.undeploySynapseConfiguration(ENDPOINT_NAME, Utils.ArtifactType.INBOUND_ENDPOINT.getDirName(), false);
    }

    @Test(groups = { "wso2.esb" },
            description = "Check whether polling is permanently suspended when limit is zero.")
    public void testPollingWithSuspensionLimitAsZero() throws Exception {

        pushMessageToQue(addEndpointWithSuspensionLimitZero());

        assertTrue(Utils.checkForLog(carbonLogReader, "Polling is suspended permanently", 180),
                   "JMS Polling is not permanently suspended though the suspension limit is 0.");
        Utils.undeploySynapseConfiguration(ENDPOINT_NAME, Utils.ArtifactType.INBOUND_ENDPOINT.getDirName(), false);
    }

    /**
     * Common method to push the message to the que.
     *
     * @param inBoundEndpoint - The OMElement of the InBoundEndpoint.
     * @throws Exception -
     */
    private void pushMessageToQue(OMElement inBoundEndpoint) throws Exception {
        JMSQueueMessageProducer sender = new JMSQueueMessageProducer(
                JMSBrokerConfigurationProvider.getInstance().getBrokerConfiguration());
        String queueName = "JMSMS";

        try {
            carbonLogReader.clearLogs();
            Utils.deploySynapseConfiguration(inBoundEndpoint, ENDPOINT_NAME,
                                             Utils.ArtifactType.INBOUND_ENDPOINT.getDirName(), false);
            String deploymentLog = "Initializing Inbound Endpoint: " + ENDPOINT_NAME;
            assertTrue(carbonLogReader.checkForLog(deploymentLog, DEFAULT_TIMEOUT), "Deployment failed.");
            carbonLogReader.clearLogs();
            sender.connect(queueName);
            sender.pushMessage("<?xml version='1.0' encoding='UTF-8'?>"
                    + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
                    + " xmlns:ser=\"http://services.samples\" xmlns:xsd=\"http://services.samples/xsd\">"
                    + "   <soapenv:Header/>" + "   <soapenv:Body>" + "      <ser:placeOrder>" + "         <ser:order>"
                    + "            <xsd:price>100</xsd:price>" + "            <xsd:quantity>2000</xsd:quantity>"
                    + "            <xsd:symbol>WSO2</xsd:symbol>" + "         </ser:order>" + "      </ser:placeOrder>"
                    + "   </soapenv:Body>" + "</soapenv:Envelope>");
            log.info("Message pushed to the JMS Queue");
        } finally {
            sender.disconnect();
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        carbonLogReader.stop();
        Utils.undeploySynapseConfiguration(ENDPOINT_NAME, Utils.ArtifactType.INBOUND_ENDPOINT.getDirName(), false);
    }

    /**
     * Adding Inbound endpoint with PollingSuspensionLimit =2 and PollingSuspensionPeriod = 3000 milliseconds.
     *
     * @return -
     * @throws Exception -
     */
    private OMElement addEndpoint() throws Exception {
        OMElement synapseConfig;
        synapseConfig = AXIOMUtil.stringToOM(
                "<inboundEndpoint xmlns=\"http://ws.apache.org/ns/synapse\"\n" + "                 name=\""
                        + ENDPOINT_NAME + "\"\n" + "                 sequence=\"request\"\n"
                        + "                 onError=\"faultSeq\"\n" + "                 protocol=\"jms\"\n"
                        + "                 suspend=\"false\">\n" + "   <parameters>\n"
                        + "      <parameter name=\"interval\">1000</parameter>\n"
                        + "      <parameter name=\"sequential\">true</parameter>\n"
                        + "      <parameter name=\"coordination\">true</parameter>\n"
                        + "      <parameter name=\"java.naming.factory.initial\">org.apache.activemq.jndi.ActiveMQInitialContextFactory</parameter>\n"
                        + "      <parameter name=\"java.naming.provider.url\">tcp://localhost:61616</parameter>\n"
                        + "      <parameter name=\"transport.jms.ConnectionFactoryJNDIName\">QueueConnectionFactory</parameter>\n"
                        + "      <parameter name=\"transport.jms.ConnectionFactoryType\">queue</parameter>\n"
                        + "      <parameter name=\"transport.jms.Destination\">JMSMS</parameter>\n"
                        + "      <parameter name=\"transport.jms.SessionTransacted\">true</parameter>\n"
                        + "      <parameter name=\"transport.jms.SessionAcknowledgement\">CLIENT_ACKNOWLEDGE</parameter>\n"
                        + "      <parameter name=\"transport.jms.CacheLevel\">3</parameter>\n"
                        + "      <parameter name=\"transport.jms.SubscriptionDurable\">false</parameter>\n"
                        + "      <parameter name=\"transport.jms.RetriesBeforeSuspension\">2</parameter>\n"
                        + "      <parameter name=\"transport.jms.PollingSuspensionPeriod\">3000</parameter>\n"
                        + "   </parameters>\n" + "</inboundEndpoint>");
        return synapseConfig;
    }

    /**
     * Adding Inbound endpoint with PollingSuspensionLimit = 0.
     *
     * @return -
     * @throws Exception -
     */
    private OMElement addEndpointWithSuspensionLimitZero() throws Exception {
        OMElement synapseConfig;
        synapseConfig = AXIOMUtil.stringToOM(
                "<inboundEndpoint xmlns=\"http://ws.apache.org/ns/synapse\"\n" + "                 name=\""
                        + ENDPOINT_NAME + "\"\n" + "                 sequence=\"request\"\n"
                        + "                 onError=\"faultSeq\"\n" + "                 protocol=\"jms\"\n"
                        + "                 suspend=\"false\">\n" + "   <parameters>\n"
                        + "      <parameter name=\"interval\">1000</parameter>\n"
                        + "      <parameter name=\"sequential\">true</parameter>\n"
                        + "      <parameter name=\"coordination\">true</parameter>\n"
                        + "      <parameter name=\"java.naming.factory.initial\">org.apache.activemq.jndi.ActiveMQInitialContextFactory</parameter>\n"
                        + "      <parameter name=\"java.naming.provider.url\">tcp://localhost:61616</parameter>\n"
                        + "      <parameter name=\"transport.jms.ConnectionFactoryJNDIName\">QueueConnectionFactory</parameter>\n"
                        + "      <parameter name=\"transport.jms.ConnectionFactoryType\">queue</parameter>\n"
                        + "      <parameter name=\"transport.jms.Destination\">JMSMS</parameter>\n"
                        + "      <parameter name=\"transport.jms.SessionTransacted\">true</parameter>\n"
                        + "      <parameter name=\"transport.jms.SessionAcknowledgement\">CLIENT_ACKNOWLEDGE</parameter>\n"
                        + "      <parameter name=\"transport.jms.CacheLevel\">3</parameter>\n"
                        + "      <parameter name=\"transport.jms.SubscriptionDurable\">false</parameter>\n"
                        + "      <parameter name=\"transport.jms.RetriesBeforeSuspension\">0</parameter>\n"
                        + "      <parameter name=\"transport.jms.PollingSuspensionPeriod\">3000</parameter>\n"
                        + "   </parameters>\n" + "</inboundEndpoint>");
        return synapseConfig;
    }

}
