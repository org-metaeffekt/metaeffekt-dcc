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
import org.springframework.shell.plugin.HistoryFileNameProvider;
import org.springframework.stereotype.Component;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DccHistoryFileNameProvider implements HistoryFileNameProvider {

    @Override
    public String getProviderName() {
        return "DCC History Filename Provider";
    }

    @Override
    public String getHistoryFileName() {
        // NOTE: the file name is intentionally named such that the file will not be created
        // the content of the file is not required and partially not correct (product name, version)
        // are not correct.
        return "logs/history/dcc-shell-cmd.log";
    }

}
