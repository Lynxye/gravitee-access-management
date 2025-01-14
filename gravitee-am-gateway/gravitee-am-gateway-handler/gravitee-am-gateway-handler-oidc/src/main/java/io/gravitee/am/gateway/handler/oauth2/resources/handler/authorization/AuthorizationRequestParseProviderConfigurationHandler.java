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
package io.gravitee.am.gateway.handler.oauth2.resources.handler.authorization;

import io.gravitee.am.gateway.handler.common.vertx.utils.UriBuilderRequest;
import io.gravitee.am.gateway.handler.oidc.service.discovery.OpenIDDiscoveryService;
import io.vertx.core.Handler;
import io.vertx.reactivex.ext.web.RoutingContext;

import static io.gravitee.am.common.utils.ConstantKeys.PROVIDER_METADATA_CONTEXT_KEY;

/**
 * Fetch OpenID Provider configuration
 *
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuthorizationRequestParseProviderConfigurationHandler implements Handler<RoutingContext> {

    private final OpenIDDiscoveryService openIDDiscoveryService;

    public AuthorizationRequestParseProviderConfigurationHandler(OpenIDDiscoveryService openIDDiscoveryService) {
        this.openIDDiscoveryService = openIDDiscoveryService;
    }

    @Override
    public void handle(RoutingContext context) {
        String basePath = UriBuilderRequest.resolveProxyRequest(context);
        context.put(PROVIDER_METADATA_CONTEXT_KEY, openIDDiscoveryService.getConfiguration(basePath));
        context.next();
    }
}
