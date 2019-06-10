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
package io.gravitee.am.gateway.handler.oidc;

import io.gravitee.am.gateway.handler.api.Protocol;
import io.gravitee.am.gateway.handler.api.ProtocolConfiguration;
import io.gravitee.am.gateway.handler.api.ProtocolProvider;
import io.gravitee.am.gateway.handler.oidc.spring.OIDCConfiguration;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OIDCProtocol implements Protocol {

    @Override
    public Class<? extends ProtocolConfiguration> configuration() {
        return OIDCConfiguration.class;
    }

    @Override
    public Class<? extends ProtocolProvider> protocolProvider() {
        return OIDCProvider.class;
    }
}