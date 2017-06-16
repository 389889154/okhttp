/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import okhttp3.internal.SingleInetAddressDns;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class EventListenerTest {
  @Rule public final MockWebServer server = new MockWebServer();

  private OkHttpClient client;
  private final RecordingEventListener listener = new RecordingEventListener();

  @Before public void setUp() {
    client = new OkHttpClient.Builder()
        .dns(new SingleInetAddressDns())
        .eventListener(listener)
        .build();
  }

  @Test public void successfulDnsLookup() throws IOException {
    server.enqueue(new MockResponse());

    Request request = new Request.Builder()
        .url(server.url("/"))
        .build();
    Response response = client.newCall(request).execute();
    assertEquals(200, response.code());
    response.body().close();

    assertEquals(Events.DnsStart, listener.eventSequence.poll());
    assertEquals("localhost", listener.dnsStartDomainName);

    assertEquals(Events.DnsEnd, listener.eventSequence.poll());
    assertEquals("localhost", listener.dnsEndDomainName);
    assertEquals(1, listener.dnsEndInetAddressList.size());
  }

  enum Events {
    DnsStart, DnsEnd
  }

  static final class RecordingEventListener extends EventListener {
    final Deque<Events> eventSequence = new ArrayDeque<>();
    String dnsStartDomainName;
    String dnsEndDomainName;
    List<InetAddress> dnsEndInetAddressList;

    @Override public void dnsStart(Call call, String domainName) {
      eventSequence.offer(Events.DnsStart);
      dnsStartDomainName = domainName;
    }

    @Override public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList,
        Throwable throwable) {
      eventSequence.offer(Events.DnsEnd);
      dnsEndDomainName = domainName;
      dnsEndInetAddressList = inetAddressList;
    }
  }
}
