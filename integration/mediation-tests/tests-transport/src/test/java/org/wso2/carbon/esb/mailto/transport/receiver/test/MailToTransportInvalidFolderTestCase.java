/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.mailto.transport.receiver.test;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.Utils;
import org.wso2.esb.integration.common.utils.servers.GreenMailServer;

import java.io.File;

import static org.testng.Assert.assertTrue;

public class MailToTransportInvalidFolderTestCase extends ESBIntegrationTest {

    private static CarbonLogReader carbonLogReader;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        OMElement mailToProxyOMElement = AXIOMUtil.stringToOM(FileUtils.readFileToString(new File(
                getESBResourceLocation() + File.separator + "mailTransport" + File.separator
                        + "mailTransportReceiver" + File.separator + "mail_transport_invalid_folder.xml")));
        Utils.deploySynapseConfiguration(mailToProxyOMElement,
                "MailTransportInvalidFolder","proxy-services",
                true);
        carbonLogReader = new CarbonLogReader();
        carbonLogReader.start();

        // Since ESB reads all unread emails one by one, we have to delete
        // the all unread emails before run the test
        GreenMailServer.deleteAllEmails("imap");
    }

    @Test(groups = { "wso2.esb" }, description = "Test email transport with invalid folder")
    public void testEmailTransportInvalidFolder() throws Exception {
        assertTrue(carbonLogReader.checkForLog("FolderABC not found", DEFAULT_TIMEOUT),
                "Couldn't find the error message in log");
    }

    @AfterClass(alwaysRun = true)
    public void deleteService() throws Exception {
        Utils.undeploySynapseConfiguration("MailTransportInvalidFolder","proxy-services", false);
        carbonLogReader.stop();
    }

}

