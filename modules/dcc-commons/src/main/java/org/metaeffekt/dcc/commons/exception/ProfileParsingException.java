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
package org.metaeffekt.dcc.commons.exception;

/**
 * Exception thrown when an exception occurs during profile parsing.
 *
 * @author Karsten Klein
 */
public class ProfileParsingException extends RuntimeException {
    public ProfileParsingException() {
    }

    public ProfileParsingException(String message) {
        super(message);
    }

    public ProfileParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProfileParsingException(Throwable cause) {
        super(cause);
    }

    public ProfileParsingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
