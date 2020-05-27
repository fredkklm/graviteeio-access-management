/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.gateway.handler.uma.resources.endpoint;

import io.gravitee.am.common.exception.oauth2.InvalidRequestException;
import io.gravitee.am.common.jwt.JWT;
import io.gravitee.am.gateway.handler.common.vertx.web.auth.handler.OAuth2AuthHandler;
import io.gravitee.am.model.Domain;
import io.gravitee.am.model.oidc.Client;
import io.gravitee.am.model.uma.ResourceSet;
import io.gravitee.am.service.ResourceSetService;
import io.gravitee.am.service.exception.ResourceSetNotFoundException;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Alexandre FARIA (contact at alexandrefaria.net)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceSetRegistrationEndpointTest {

    @Mock
    private Domain domain;

    @Mock
    private ResourceSetService service;

    @Mock
    private JWT jwt;

    @Mock
    private Client client;

    @Mock
    private RoutingContext context;

    @Mock
    private HttpServerResponse response;

    @Mock
    private HttpServerRequest request;

    @InjectMocks
    private ResourceSetRegistrationEndpoint endpoint = new ResourceSetRegistrationEndpoint(domain, service);

    private static final String DOMAIN_PATH = "domain";
    private static final String DOMAIN_ID = "123";
    private static final String USER_ID = "456";
    private static final String CLIENT_ID = "api";
    private static final String RESOURCE_ID = "rs_id";

    ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Throwable> errCaptor = ArgumentCaptor.forClass(Throwable.class);

    @Before
    public void setUp() {
        when(domain.getId()).thenReturn(DOMAIN_ID);
        when(domain.getPath()).thenReturn(DOMAIN_PATH);
        when(jwt.getSub()).thenReturn(USER_ID);
        when(client.getId()).thenReturn(CLIENT_ID);
        when(context.get(OAuth2AuthHandler.TOKEN_CONTEXT_KEY)).thenReturn(jwt);
        when(context.get(OAuth2AuthHandler.CLIENT_CONTEXT_KEY)).thenReturn(client);
        when(context.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(context.request()).thenReturn(request);
        when(request.getParam("resource_id")).thenReturn(RESOURCE_ID);
    }

    @Test
    public void list_anyError() {
        when(service.listByDomainAndClientAndUser(anyString(), anyString(), anyString())).thenReturn(Single.error(new RuntimeException()));
        endpoint.handle(context);
        verify(context, times(1)).fail(errCaptor.capture());
        Assert.assertTrue("Error must be propagated", errCaptor.getValue() instanceof RuntimeException);
    }

    @Test
    public void list_noResourceSet() {
        when(service.listByDomainAndClientAndUser(DOMAIN_ID, CLIENT_ID, USER_ID)).thenReturn(Single.just(Collections.emptyList()));
        endpoint.handle(context);
        verify(response, times(1)).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        verify(response, times(1)).setStatusCode(intCaptor.capture());
        Assert.assertEquals("Should be no content status",204, intCaptor.getValue().intValue());
    }

    @Test
    public void list_withResourceSet() {
        when(service.listByDomainAndClientAndUser(DOMAIN_ID, CLIENT_ID, USER_ID)).thenReturn(Single.just(Arrays.asList(new ResourceSet().setId(RESOURCE_ID))));
        endpoint.handle(context);
        verify(response, times(1)).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        verify(response, times(1)).setStatusCode(intCaptor.capture());
        Assert.assertEquals("Should be ok",200, intCaptor.getValue().intValue());
    }


    @Test
    public void create_invalidResourceSetBody() {
        when(context.getBodyAsJson()).thenReturn(new JsonObject(Json.encode(new ResourceSet().setId(RESOURCE_ID))));
        endpoint.create(context);
        verify(context).fail(errCaptor.capture());
        Assert.assertTrue(errCaptor.getValue() instanceof InvalidRequestException);
    }

    @Test
    public void create_noResourceSet() {
        when(context.getBodyAsJson()).thenReturn(new JsonObject("{\"id\":\"rs_id\",\"resource_scopes\":[\"scope\"]}"));
        when(service.create(any() , eq(DOMAIN_ID), eq(CLIENT_ID), eq(USER_ID))).thenReturn(Single.error(new ResourceSetNotFoundException(RESOURCE_ID)));
        endpoint.create(context);
        verify(context).fail(errCaptor.capture());
        Assert.assertTrue(errCaptor.getValue() instanceof ResourceSetNotFoundException);
    }

    @Test
    public void create_withResourceSet() {
        ArgumentCaptor<String> strCaptor = ArgumentCaptor.forClass(String.class);
        when(context.getBodyAsJson()).thenReturn(new JsonObject("{\"id\":\"rs_id\",\"resource_scopes\":[\"scope\"]}"));
        when(service.create(any() , eq(DOMAIN_ID), eq(CLIENT_ID), eq(USER_ID))).thenReturn(Single.just(new ResourceSet().setId(RESOURCE_ID)));
        when(request.host()).thenReturn("host");
        when(request.scheme()).thenReturn("http");
        endpoint.create(context);
        verify(response, times(1)).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        verify(response, times(1)).putHeader(eq(HttpHeaders.LOCATION),strCaptor.capture());
        verify(response, times(1)).setStatusCode(intCaptor.capture());
        Assert.assertEquals("Should be created",201, intCaptor.getValue().intValue());
        Assert.assertEquals("Location", "http://host/"+DOMAIN_PATH+"/uma/protection/resource_set/"+RESOURCE_ID, strCaptor.getValue());
    }

    @Test
    public void get_noResourceSet() {
        when(service.findByDomainAndClientAndUserAndResource(DOMAIN_ID, CLIENT_ID, USER_ID, RESOURCE_ID)).thenReturn(Maybe.empty());
        endpoint.get(context);
        verify(context).fail(errCaptor.capture());
        Assert.assertTrue(errCaptor.getValue() instanceof ResourceSetNotFoundException);
    }

    @Test
    public void get_withResourceSet() {
        when(service.findByDomainAndClientAndUserAndResource(DOMAIN_ID, CLIENT_ID, USER_ID, RESOURCE_ID)).thenReturn(Maybe.just(new ResourceSet().setId(RESOURCE_ID)));
        endpoint.get(context);
        verify(response, times(1)).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        verify(response, times(1)).setStatusCode(intCaptor.capture());
        Assert.assertEquals("Should be ok",200, intCaptor.getValue().intValue());
    }

    @Test
    public void update_invalidResourceSetBody() {
        when(context.getBodyAsJson()).thenReturn(new JsonObject("{\"description\":\"mydescription\"}"));
        endpoint.update(context);
        verify(context).fail(errCaptor.capture());
        Assert.assertTrue(errCaptor.getValue() instanceof InvalidRequestException);
    }

    @Test
    public void update_noResourceSet() {
        when(context.getBodyAsJson()).thenReturn(new JsonObject("{\"id\":\"rs_id\",\"resource_scopes\":[\"scope\"]}"));
        when(service.update(any() , eq(DOMAIN_ID), eq(CLIENT_ID), eq(USER_ID), eq(RESOURCE_ID))).thenReturn(Single.error(new ResourceSetNotFoundException(RESOURCE_ID)));
        endpoint.update(context);
        verify(context).fail(errCaptor.capture());
        Assert.assertTrue(errCaptor.getValue() instanceof ResourceSetNotFoundException);
    }

    @Test
    public void update_withResourceSet() {
        when(context.getBodyAsJson()).thenReturn(new JsonObject("{\"id\":\"rs_id\",\"resource_scopes\":[\"scope\"]}"));
        when(service.update(any() , eq(DOMAIN_ID), eq(CLIENT_ID), eq(USER_ID), eq(RESOURCE_ID))).thenReturn(Single.just(new ResourceSet()));
        endpoint.update(context);
        verify(response, times(1)).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        verify(response, times(1)).setStatusCode(intCaptor.capture());
        Assert.assertEquals("Should be ok",200, intCaptor.getValue().intValue());
    }

    @Test
    public void delete_noResourceSet() {
        when(service.delete(DOMAIN_ID, CLIENT_ID, USER_ID, RESOURCE_ID)).thenReturn(Completable.error(new ResourceSetNotFoundException(RESOURCE_ID)));
        endpoint.delete(context);
        verify(context).fail(errCaptor.capture());
        Assert.assertTrue(errCaptor.getValue() instanceof ResourceSetNotFoundException);
    }

    @Test
    public void delete_withResourceSet() {
        when(service.delete(DOMAIN_ID, CLIENT_ID, USER_ID, RESOURCE_ID)).thenReturn(Completable.complete());
        endpoint.delete(context);
        verify(response, times(1)).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        verify(response, times(1)).setStatusCode(intCaptor.capture());
        Assert.assertEquals("Should be no content status",204, intCaptor.getValue().intValue());
    }
}