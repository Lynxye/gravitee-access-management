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
package io.gravitee.am.gateway.handler.root.resources.endpoint.login;

import io.gravitee.am.common.utils.ConstantKeys;
import io.gravitee.am.common.oidc.Parameters;
import io.gravitee.am.gateway.handler.common.vertx.core.http.VertxHttpServerRequest;
import io.gravitee.am.gateway.handler.context.EvaluableRequest;
import io.gravitee.am.gateway.handler.manager.botdetection.BotDetectionManager;
import io.gravitee.am.gateway.handler.manager.deviceidentifiers.DeviceIdentifierManager;
import io.gravitee.am.gateway.handler.root.resources.endpoint.AbstractEndpoint;
import io.gravitee.am.model.Domain;
import io.gravitee.am.model.Template;
import io.gravitee.am.model.login.LoginSettings;
import io.gravitee.am.model.oidc.Client;
import io.gravitee.am.model.safe.ClientProperties;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.common.template.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.gravitee.am.common.utils.ConstantKeys.ACTION_KEY;
import static io.gravitee.am.gateway.handler.common.utils.ThymeleafDataHelper.generateData;
import static io.gravitee.am.gateway.handler.common.vertx.utils.RequestUtils.getCleanedQueryParams;
import static io.gravitee.am.gateway.handler.common.vertx.utils.UriBuilderRequest.CONTEXT_PATH;
import static io.gravitee.am.gateway.handler.common.vertx.utils.UriBuilderRequest.resolveProxyRequest;
import static java.util.Optional.ofNullable;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoginEndpoint extends AbstractEndpoint implements Handler<RoutingContext> {

    private static final Logger logger = LoggerFactory.getLogger(LoginEndpoint.class);
    private static final String ALLOW_FORGOT_PASSWORD_CONTEXT_KEY = "allowForgotPassword";
    private static final String ALLOW_REGISTER_CONTEXT_KEY = "allowRegister";
    private static final String ALLOW_PASSWORDLESS_CONTEXT_KEY = "allowPasswordless";
    private static final String HIDE_FORM_CONTEXT_KEY = "hideLoginForm";
    private static final String IDENTIFIER_FIRST_LOGIN_CONTEXT_KEY = "identifierFirstLoginEnabled";
    private static final String REQUEST_CONTEXT_KEY = "request";
    private static final String FORGOT_ACTION_KEY = "forgotPasswordAction";
    private static final String REGISTER_ACTION_KEY = "registerAction";
    private static final String WEBAUTHN_ACTION_KEY = "passwordlessAction";
    private static final String LOGIN_IDENTIFIER_ACTION_KEY = "backToLoginIdentifierAction";

    private final Domain domain;
    private final BotDetectionManager botDetectionManager;
    private final DeviceIdentifierManager deviceIdentifierManager;

    public LoginEndpoint(TemplateEngine templateEngine, Domain domain, BotDetectionManager botDetectionManager, DeviceIdentifierManager deviceIdentifierManager) {
        super(templateEngine);
        this.domain = domain;
        this.botDetectionManager = botDetectionManager;
        this.deviceIdentifierManager = deviceIdentifierManager;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        final Client client = routingContext.get(ConstantKeys.CLIENT_CONTEXT_KEY);
        prepareContext(routingContext, client);
        renderLoginPage(routingContext, client);
    }

    private void prepareContext(RoutingContext routingContext, Client client) {
        // remove sensible client data
        routingContext.put(ConstantKeys.CLIENT_CONTEXT_KEY, new ClientProperties(client));
        // put domain in context data
        routingContext.put(ConstantKeys.DOMAIN_CONTEXT_KEY, domain);
        // put login settings in context data
        LoginSettings loginSettings = LoginSettings.getInstance(domain, client);
        var optionalSettings = ofNullable(loginSettings).filter(Objects::nonNull);
        boolean isIdentifierFirstLoginEnabled = optionalSettings.map(LoginSettings::isIdentifierFirstEnabled).orElse(false);
        routingContext.put(ALLOW_FORGOT_PASSWORD_CONTEXT_KEY, optionalSettings.map(LoginSettings::isForgotPasswordEnabled).orElse(false));
        routingContext.put(ALLOW_REGISTER_CONTEXT_KEY, optionalSettings.map(LoginSettings::isRegisterEnabled).orElse(false));
        routingContext.put(ALLOW_PASSWORDLESS_CONTEXT_KEY, optionalSettings.map(LoginSettings::isPasswordlessEnabled).orElse(false));
        routingContext.put(HIDE_FORM_CONTEXT_KEY, optionalSettings.map(LoginSettings::isHideForm).orElse(false));
        routingContext.put(IDENTIFIER_FIRST_LOGIN_CONTEXT_KEY, isIdentifierFirstLoginEnabled);

        // put request in context
        EvaluableRequest evaluableRequest = new EvaluableRequest(new VertxHttpServerRequest(routingContext.request().getDelegate(), true));
        routingContext.put(REQUEST_CONTEXT_KEY, evaluableRequest);

        // put error in context
        final String error = routingContext.request().getParam(ConstantKeys.ERROR_PARAM_KEY);
        final String errorDescription = routingContext.request().getParam(ConstantKeys.ERROR_DESCRIPTION_PARAM_KEY);
        routingContext.put(ConstantKeys.ERROR_PARAM_KEY, error);
        routingContext.put(ConstantKeys.ERROR_DESCRIPTION_PARAM_KEY, errorDescription);

        // put parameters in context (backward compatibility)
        Map<String, String> params = new HashMap<>(evaluableRequest.getParams().toSingleValueMap());
        params.put(ConstantKeys.ERROR_PARAM_KEY, error);
        params.put(ConstantKeys.ERROR_DESCRIPTION_PARAM_KEY, errorDescription);
        final String loginHint = routingContext.request().getParam(Parameters.LOGIN_HINT);
        if (loginHint != null) {
            params.put(ConstantKeys.USERNAME_PARAM_KEY, loginHint);
        }
        routingContext.put(ConstantKeys.PARAM_CONTEXT_KEY, params);

        // put action urls in context
        final MultiMap queryParams = getCleanedQueryParams(routingContext.request());
        routingContext.put(ACTION_KEY, resolveProxyRequest(routingContext.request(), routingContext.request().path(), queryParams, true));
        routingContext.put(FORGOT_ACTION_KEY, resolveProxyRequest(routingContext.request(), routingContext.get(CONTEXT_PATH) + "/forgotPassword", queryParams, true));
        routingContext.put(REGISTER_ACTION_KEY, resolveProxyRequest(routingContext.request(), routingContext.get(CONTEXT_PATH) + "/register", queryParams, true));
        routingContext.put(WEBAUTHN_ACTION_KEY, resolveProxyRequest(routingContext.request(), routingContext.get(CONTEXT_PATH) + "/webauthn/login", queryParams, true));
        if (isIdentifierFirstLoginEnabled) {
            routingContext.put(LOGIN_IDENTIFIER_ACTION_KEY, resolveProxyRequest(routingContext.request(), routingContext.get(CONTEXT_PATH) + "/login/identifier", queryParams, true));
        }
    }

    private void renderLoginPage(RoutingContext routingContext, Client client) {
        // we redirect if we don't have the username while being on first login identifier enabled
        boolean isIdentifierFirstLoginEnabled = routingContext.get(IDENTIFIER_FIRST_LOGIN_CONTEXT_KEY);
        String username = routingContext.request().getParam(Parameters.LOGIN_HINT);
        if (isIdentifierFirstLoginEnabled && StringUtils.isEmpty(username)) {
            final String redirectUrl = routingContext.get(LOGIN_IDENTIFIER_ACTION_KEY);
            routingContext.response()
                    .putHeader(io.vertx.core.http.HttpHeaders.LOCATION, redirectUrl)
                    .setStatusCode(302)
                    .end();
        } else {
            final Map<String, Object> data = generateData(routingContext, domain, client);
            data.putAll(botDetectionManager.getTemplateVariables(domain, client));
            data.putAll(deviceIdentifierManager.getTemplateVariables(client));
            renderPage(routingContext, data, client, logger, "Unable to render login page");
        }
    }

    @Override
    public String getTemplateSuffix() {
        return Template.LOGIN.template();
    }
}
