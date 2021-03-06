/*
 * Copyright (c) 2016 HERE Europe B.V.
 * 
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
package com.here.account.http.apache;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;

public class ApacheHttpClientProviderTest {
    
    HttpRequestAuthorizer httpRequestAuthorizer;
    
    @Before
    public void setUp() {
        httpRequestAuthorizer = mock(HttpRequestAuthorizer.class);
        doAnswer(new Answer<Void>() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            //headers.put((String)invocation.getArguments()[0], (String)invocation.getArguments()[1]);
            return null;
          }
        }).when(httpRequestAuthorizer).authorize(any(HttpRequest.class), any(String.class), any(String.class), 
                (Map<String, List<String>>) any(Map.class));

        httpProvider = ApacheHttpClientProvider.builder().build();
        url = "http://localhost:8080/path/to";
        formParams = null;
    }
    
    @Test
    public void test_javadocs() throws IOException {
        
        HttpProvider httpProvider = ApacheHttpClientProvider.builder().build();
        // use httpProvider such as with HereAccessTokenProviders...
        
        assertTrue("httpProvider was null", null != httpProvider);
        httpProvider.close();
    }
    
    HttpRequest httpRequest;
    HttpProvider httpProvider;
    String url;
    Map<String, List<String>> formParams;
    
    @Test
    public void test_badUri() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        url = "htp:/ asdf8080:a:z";
        try {
            httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "GET", url, formParams);
            fail("should have thrown exception for url "+url+", but didn't");
        } catch (IllegalArgumentException e) {
            // expected
            String message = e.getMessage();
            String expectedContains = "malformed URL";
            assertTrue("expected contains "+expectedContains+", actual "+message, message.contains(expectedContains));
        }
    }
    
    @Test
    public void test_methodDoesntSupportFormParams() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        formParams = new HashMap<String, List<String>>();
        formParams.put("foo", Collections.singletonList("bar"));
        try {
            httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "DELETE", url, formParams);
            fail("should have thrown exception for formParams with method DELETE, but didn't");
        } catch (IllegalArgumentException e) {
            // expected
            String message = e.getMessage();
            String expectedContains = "no formParams permitted for method";
            assertTrue("expected contains "+expectedContains+", actual "+message, message.contains(expectedContains));
        }
    }
    
    @Test
    public void test_formParamsPut() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        formParams = new HashMap<String, List<String>>();
        formParams.put("foo", Collections.singletonList("bar"));
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "PUT", url, formParams);
        HttpRequestBase httpRequestBase = getHttpRequestBase();
        HttpPut httpPut = (HttpPut) httpRequestBase;
        HttpEntity httpEntity = httpPut.getEntity();
        assertTrue("httpEntity was null", null != httpEntity);
    }

    @Test
    public void test_formParamsPut_null() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        formParams = null;
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "PUT", url, formParams);
        HttpRequestBase httpRequestBase = getHttpRequestBase();
        HttpPut httpPut = (HttpPut) httpRequestBase;
        HttpEntity httpEntity = httpPut.getEntity();
        assertTrue("httpEntity was expected null, actual "+httpEntity, null == httpEntity);
    }

    
    @Test
    public void test_methodDoesntSupportJson() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        String requestBodyJson = "{\"foo\":\"bar\"}";
        try {
            httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "DELETE", url, requestBodyJson);
            fail("should have thrown exception for JSON body with method DELETE, but didn't");
        } catch (IllegalArgumentException e) {
            // expected
            String message = e.getMessage();
            String expectedContains = "no JSON request body permitted for method";
            assertTrue("expected contains "+expectedContains+", actual "+message, message.contains(expectedContains));
        }
    }

    
    @Test
    public void test_jsonPut() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        String requestBodyJson = "{\"foo\":\"bar\"}";
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "PUT", url, requestBodyJson);
        HttpRequestBase httpRequestBase = getHttpRequestBase();
        HttpPut httpPut = (HttpPut) httpRequestBase;
        HttpEntity httpEntity = httpPut.getEntity();
        assertTrue("httpEntity was null", null != httpEntity);
    }
    
    @Test
    public void test_jsonPut_null() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        String requestBodyJson = null;
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, "PUT", url, requestBodyJson);
        HttpRequestBase httpRequestBase = getHttpRequestBase();
        HttpPut httpPut = (HttpPut) httpRequestBase;
        HttpEntity httpEntity = httpPut.getEntity();
        assertTrue("httpEntity was expected null, but was "+httpEntity, null == httpEntity);
    }



    
    @Test
    public void test_methods() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, ClassNotFoundException {
        verifyApacheType("GET", HttpGet.class);
        verifyApacheType("POST", HttpPost.class);
        verifyApacheType("PUT", HttpPut.class);
        verifyApacheType("DELETE", HttpDelete.class);
        verifyApacheType("HEAD", HttpHead.class);
        verifyApacheType("OPTIONS", HttpOptions.class);
        verifyApacheType("TRACE", HttpTrace.class);
        verifyApacheType("PATCH", HttpPatch.class);
        try {
            verifyApacheType("BROKENMETHOD", null);
            fail("BROKENMETHOD should have thrown IllegalArgumentException, but didn't");
        } catch (IllegalArgumentException e) {
            // expected
            String message = e.getMessage();
            String expectedContains = "no support for request method=BROKENMETHOD";
            assertTrue("expected contains "+expectedContains+", actual "+message, message.contains(expectedContains));
        }
    }
    
    protected void verifyApacheType(String method, Class<?> clazz) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, ClassNotFoundException {
        httpRequest = httpProvider.getRequest(httpRequestAuthorizer, method, url, formParams);

        Class<?> expectedType = Class.forName("com.here.account.http.apache.ApacheHttpClientProvider$ApacheHttpClientRequest");
        Class<?> actualType = httpRequest.getClass();
        assertTrue("httpRequest was not expected "+expectedType+", actual "+actualType, expectedType.equals(actualType));
        
        HttpRequestBase o = getHttpRequestBase();
        expectedType = clazz;
        actualType = o.getClass();
        assertTrue("o was wrong type, expected "+expectedType+", actual "+actualType, expectedType.equals(actualType));
    }
    
    protected HttpRequestBase getHttpRequestBase() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Class<?> actualType = httpRequest.getClass();
        Field field = actualType.getDeclaredField("httpRequestBase");
        assertTrue("field was null", null != field);
        field.setAccessible(true);
        Object o = field.get(httpRequest);
        assertTrue("o was null", null != o);
        assertTrue("o wasn't an HttpRequestBase", HttpRequestBase.class.isAssignableFrom(o.getClass()));

        return (HttpRequestBase) o;
    }
}
