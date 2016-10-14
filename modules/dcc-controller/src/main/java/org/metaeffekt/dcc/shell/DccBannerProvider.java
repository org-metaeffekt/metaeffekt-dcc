/**
 * Copyright 2009-2016 the original author or authors.
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
package org.metaeffekt.dcc.shell;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.BannerProvider;
import org.springframework.stereotype.Component;

import org.metaeffekt.dcc.controller.DccControllerConstants;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DccBannerProvider implements BannerProvider {

    private static final String CURRENT_VERSION = DccControllerConstants.DCC_SHELL_VERSION;
    private static String LINE_SEPARATOR = System.getProperty("line.separator");
    
    @Override
    public String getProviderName() {
        return "DCC Shell";
    }

    @Override
    public String getBanner() {
        StringBuilder sb = new StringBuilder();
        sb.append("    ____   ______ ______   _____  __           __ __").append(LINE_SEPARATOR);
        sb.append("   / __ \\ / ____// ____/  / ___/ / /_   ___   / // /").append(LINE_SEPARATOR);
        sb.append("  / / / // /    / /       \\__ \\ / __ \\ / _ \\ / // /").append(LINE_SEPARATOR);
        sb.append(" / /_/ // /___ / /___    ___/ // / / //  __// // /").append(LINE_SEPARATOR);
        sb.append("/_____/ \\____/ \\____/   /____//_/ /_/ \\___//_//_/").append(LINE_SEPARATOR);
        return sb.toString();
    }
    
    @Override
    public String getVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public String getWelcomeMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Welcome to the DCC Shell.");
        sb.append(LINE_SEPARATOR);
        sb.append("Hit TAB to see a list of options, enter [help] for help and [exit] to exit.");
        return sb.toString();
    }

}
