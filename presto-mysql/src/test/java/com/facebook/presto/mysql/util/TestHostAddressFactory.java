/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.mysql.util;

import com.facebook.presto.spi.HostAddress;
import org.testng.annotations.Test;
import java.util.Collection;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class TestHostAddressFactory
{
    @Test
    public void testToHostAddressList()
            throws Exception
    {
        Collection<MySQLHost> hosts = null;

        HostAddressFactory hostAddressFactory = new HostAddressFactory();
        List<HostAddress> list = hostAddressFactory.toHostAddressList(hosts);

        assertEquals("[[102:304:506:708:90a:b0c:d0e:f10], 1.2.3.4]", list.toString());
    }
}
