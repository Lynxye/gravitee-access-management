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
package io.gravitee.am.service.reporter.builder.management;

import io.gravitee.am.common.audit.EntityType;
import io.gravitee.am.common.audit.EventType;
import io.gravitee.am.common.utils.ConstantKeys;
import io.gravitee.am.model.User;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserAuditBuilder extends ManagementAuditBuilder<UserAuditBuilder> {

    public UserAuditBuilder() {
        super();
    }

    public UserAuditBuilder user(User user) {
        if (user != null) {
            if (EventType.USER_CREATED.equals(getType())
                    || EventType.USER_UPDATED.equals(getType())
                    || EventType.USER_ROLES_ASSIGNED.equals(getType())) {
                setNewValue(user);
            }

            referenceType(user.getReferenceType());
            referenceId(user.getReferenceId());

            setTarget(user.getId(), EntityType.USER, user.getUsername(), getDisplayName(user), user.getReferenceType(), user.getReferenceId());
        }
        return this;
    }

    private String getDisplayName(User user) {
        return user.getDisplayName() != null ? user.getDisplayName() :
                user.getFirstName() != null ? user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "") :
                        user.getUsername();
    }

    @Override
    protected Object removeSensitiveData(Object value) {
        if (value != null && value instanceof User) {
            User safeUser = new User((User)value);
            safeUser.setPassword(null);
            safeUser.setRegistrationAccessToken(null);
            if (safeUser.getAdditionalInformation() != null) {
                safeUser.getAdditionalInformation().remove(ConstantKeys.OIDC_PROVIDER_ID_ACCESS_TOKEN_KEY);
                safeUser.getAdditionalInformation().remove(ConstantKeys.OIDC_PROVIDER_ID_TOKEN_KEY);
            }
            return safeUser;
        }
        return value;
    }
}
