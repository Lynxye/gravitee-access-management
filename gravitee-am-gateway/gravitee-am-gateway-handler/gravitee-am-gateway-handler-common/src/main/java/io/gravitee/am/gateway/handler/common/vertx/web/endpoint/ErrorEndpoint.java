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
package io.gravitee.am.gateway.handler.common.vertx.web.endpoint;

import io.gravitee.am.common.jwt.JWT;
import io.gravitee.am.common.oauth2.Parameters;
import io.gravitee.am.gateway.handler.common.client.ClientSyncService;
import io.gravitee.am.gateway.handler.common.jwt.JWTService;
import io.gravitee.am.gateway.handler.common.utils.ThymeleafDataHelper;
import io.gravitee.am.model.Domain;
import io.gravitee.am.model.oidc.Client;
import io.gravitee.am.model.Template;
import io.gravitee.am.service.exception.ClientNotFoundException;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static io.gravitee.am.gateway.handler.common.utils.ThymeleafDataHelper.generateData;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ErrorEndpoint implements Handler<RoutingContext> {

    private static final Logger logger = LoggerFactory.getLogger(ErrorEndpoint.class);
    private static final String ERROR_PARAM = "error";
    private static final String ERROR_DESCRIPTION_PARAM = "error_description";
    private static final String PARAM_CONTEXT_KEY = "param";
    private Domain domain;
    private ThymeleafTemplateEngine engine;
    private ClientSyncService clientSyncService;
    private JWTService jwtService;

    public ErrorEndpoint(Domain domain, ThymeleafTemplateEngine engine, ClientSyncService clientSyncService, JWTService jwtService) {
        this.domain = domain;
        this.engine = engine;
        this.clientSyncService = clientSyncService;
        this.jwtService = jwtService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        final HttpServerRequest request = routingContext.request();
        final String clientId = request.getParam(Parameters.CLIENT_ID);

        if (clientId == null) {
            renderErrorPage(routingContext, null);
            return;
        }

        // fetch client to display its own custom page
        resolveClient(clientId, handler -> {
            if (handler.failed()) {
                // an error occurs while fetching the client
                // we will display the domain error page
                // log this error for the prosperity
                logger.debug("An error occurs while fetching client {}", clientId, handler.cause());
                renderErrorPage(routingContext, null);
                return;
            }

            renderErrorPage(routingContext, handler.result());
        });
    }

    private void renderErrorPage(RoutingContext routingContext, Client client) {
        final HttpServerRequest request = routingContext.request();
        final String error = request.getParam(ERROR_PARAM);
        String errorDescription = request.getParam(ERROR_DESCRIPTION_PARAM);
        if (errorDescription != null) {
            try {
                errorDescription = java.net.URLDecoder.decode(request.getParam(ERROR_DESCRIPTION_PARAM), StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                // unable to decode UTF-8 encoded query parameter
            }
        }

        final Map<String, String> errorParams = new HashMap<>();
        errorParams.put(ERROR_PARAM, error);
        errorParams.put(ERROR_DESCRIPTION_PARAM, errorDescription);

        Single<Map<String, String>> singlePageRendering = Single.just(errorParams);

        final String jarm = request.getParam(io.gravitee.am.common.oidc.Parameters.RESPONSE);
        if (error == null && jarm != null) {
            // extract error details from the JWT provided as response parameter
            singlePageRendering = this.jwtService.decode(jarm).map(jwt -> {
                Map<String, String> result = new HashMap<>();
                result.put(ERROR_PARAM, (String) jwt.get(ERROR_PARAM));
                result.put(ERROR_DESCRIPTION_PARAM, (String) jwt.get(ERROR_DESCRIPTION_PARAM));
                return result;
            });
        }

        singlePageRendering.subscribe(
                params -> render(routingContext, client, params),
                // single contains an error due to JWT decoding, return the default error page without error details
                (exception) ->render(routingContext, client, errorParams));

    }

    private void render(RoutingContext routingContext, Client client, Map<String, String> params) {
        params.forEach((k, v) -> routingContext.put(k, v));
        // put parameters in context (backward compatibility)
        routingContext.put(PARAM_CONTEXT_KEY, params);

        engine.render(generateData(routingContext, domain, client), getTemplateFileName(client), res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML);
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

    private String getTemplateFileName(Client client) {
        return Template.ERROR.template() + (client != null ? "|" + client.getId() : "");
    }

    private void resolveClient(String clientId, Handler<AsyncResult<Client>> handler) {
        clientSyncService.findByDomainAndClientId(domain.getId(), clientId)
                .subscribe(
                        client -> handler.handle(Future.succeededFuture(client)),
                        error -> handler.handle(Future.failedFuture(error)),
                        () -> handler.handle(Future.failedFuture(new ClientNotFoundException(clientId)))
                );
    }
}
