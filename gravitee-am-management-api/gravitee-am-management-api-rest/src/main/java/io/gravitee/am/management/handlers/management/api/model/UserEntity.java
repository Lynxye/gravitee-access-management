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
package io.gravitee.am.management.handlers.management.api.model;

import io.gravitee.am.model.User;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserEntity extends User {
    public static final String MD_SOURCE_ID = "sourceId";

    private ApplicationEntity applicationEntity;

    private Map<String, Object> metadata;

    public UserEntity(User user){
        this(user, false);
    }

    public UserEntity(User user, boolean withMetadata) {
        setId(user.getId());
        setExternalId(user.getExternalId());
        setUsername(user.getUsername());
        setPassword(null);
        setEmail(user.getEmail());
        setDisplayName(user.getDisplayName());
        setNickName(user.getNickName());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
        setTitle(user.getTitle());
        setPicture(user.getPicture());
        setAccountNonExpired(user.isAccountNonExpired());
        setAccountNonLocked(user.isAccountNonLocked());
        setAccountLockedAt(user.getAccountLockedAt());
        setAccountLockedUntil(user.getAccountLockedUntil());
        setCredentialsNonExpired(user.isCredentialsNonExpired());
        setEnabled(user.isEnabled());
        setInternal(user.isInternal());
        setPreRegistration(user.isPreRegistration());
        setRegistrationCompleted(user.isRegistrationCompleted());
        setReferenceType(user.getReferenceType());
        setReferenceId(user.getReferenceId());
        setSource(user.getSource());
        setClient(user.getClient());
        setLoginsCount(user.getLoginsCount());
        setLoggedAt(user.getLoggedAt());
        setRoles(user.getRoles());
        setDynamicRoles(user.getDynamicRoles());
        setRolesPermissions(user.getRolesPermissions());
        setAdditionalInformation(user.getAdditionalInformation());
        setCreatedAt(user.getCreatedAt());
        setUpdatedAt(user.getUpdatedAt());
        if (withMetadata) {
            this.metadata = new HashMap<>();
            this.metadata.put(MD_SOURCE_ID, user.getSource());
        }
    }

    public ApplicationEntity getApplicationEntity() {
        return applicationEntity;
    }

    public void setApplicationEntity(ApplicationEntity applicationEntity) {
        this.applicationEntity = applicationEntity;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

}