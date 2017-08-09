/**
 * Copyright 2009-2017 the original author or authors.
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

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.PromptProvider;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DccPromptProvider implements PromptProvider {

    public static final String PROMPT_END = "> ";
    public static final String DCC_PROMPT = "dcc";
    public static final String DCC_PROMPT_SEPARATOR = "#";
    private static final String DEFAULT_PROMPT = DCC_PROMPT + PROMPT_END;
    
    private String prompt = DEFAULT_PROMPT;
    
    @Override
    public String getProviderName() {
        return "DccPromptProvider";
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        if (StringUtils.isNotEmpty(prompt)) {
            if (prompt.endsWith(PROMPT_END)) {
                this.prompt = prompt;
            } else {
                this.prompt = prompt + PROMPT_END;
            }
        } else {
            this.prompt = DEFAULT_PROMPT;
        }
    }
    
    public void resetPrompt() {
        prompt = DEFAULT_PROMPT;
    }

}
