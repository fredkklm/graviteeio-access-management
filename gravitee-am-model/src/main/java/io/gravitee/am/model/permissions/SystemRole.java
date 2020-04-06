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
package io.gravitee.am.model.permissions;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum SystemRole {
    PLATFORM_ADMIN(true),
    ORGANIZATION_ADMIN(false), ORGANIZATION_PRIMARY_OWNER(true), ORGANIZATION_USER(false),
    DOMAIN_ADMIN(false), DOMAIN_PRIMARY_OWNER(true), DOMAIN_USER(false),
    ENVIRONMENT_ADMIN(false), ENVIRONMENT_PRIMARY_OWNER(true),
    APPLICATION_ADMIN(false), APPLICATION_PRIMARY_OWNER(true), APPLICATION_USER(false);

    private boolean internalOnly;

    SystemRole(boolean internalOnly) {
        this.internalOnly = internalOnly;
    }

    public boolean isInternalOnly() {
        return internalOnly;
    }
}